#!/usr/bin/env bash
set -eo pipefail
if [ -z "$KRS_APPLICATION_TEST_PASSWORD" ]; then echo "Missing KRS application test password" >&2; exit 1; fi
if [ -z "$KRS_APPLICATION_SESSION_ID" ]; then echo "Missing KRS application test session ID" >&2; exit 1; fi
BASE_URL="http://127.0.0.1:8081"
if [ "$#" -gt 0 ]; then BASE_URL="$1"; fi
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

csrf_from_file() {
  python3 - "$1" <<'PY'
import sys
from html.parser import HTMLParser
class Parser(HTMLParser):
    token = None
    def handle_starttag(self, tag, attrs):
        values = dict(attrs)
        if tag.lower() == "input" and values.get("name") == "csrf_token":
            self.token = values.get("value")
parser = Parser()
with open(sys.argv[1], encoding="utf-8") as source:
    parser.feed(source.read())
if not parser.token:
    raise SystemExit("CSRF token not found")
print(parser.token)
PY
}

login_student() {
  local username="$1" jar="$2"
  local page="$WORK_DIR/login.html" token status
  curl --silent --show-error --cookie-jar "$jar" "$BASE_URL/" -o "$page"
  token="$(csrf_from_file "$page")"
  status="$(curl --silent --show-error --cookie "$jar" --cookie-jar "$jar" \
    --output "$WORK_DIR/login-response.html" --write-out '%{http_code}' \
    --data-urlencode "csrf_token=$token" \
    --data-urlencode 'role=Mahasiswa' \
    --data-urlencode "username=$username" \
    --data-urlencode "password=$KRS_APPLICATION_TEST_PASSWORD" "$BASE_URL/")"
  if [[ "$status" != "302" && "$status" != "303" ]]; then
    echo "Student login failed (HTTP $status)" >&2
    exit 1
  fi
}

extract_application_id() {
  python3 - "$1" <<'PY'
import re, sys
from html.parser import HTMLParser
class Links(HTMLParser):
    found = None
    def handle_starttag(self, tag, attrs):
        if tag.lower() == "a":
            href = dict(attrs).get("href", "")
            match = re.search(r"pengajuan\.php\?detail=([0-9]+)", href)
            if match:
                self.found = match.group(1)
parser = Links()
with open(sys.argv[1], encoding="utf-8") as source:
    parser.feed(source.read())
if not parser.found:
    raise SystemExit("Could not find student application detail link")
print(parser.found)
PY
}

student_jar="$WORK_DIR/student-1.cookies"
login_student "KRS Application Student" "$student_jar"

status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/dashboard.html" --write-out '%{http_code}' "$BASE_URL/")"
[[ "$status" == "200" ]]
grep -Fq 'href="pengajuan.php"' "$WORK_DIR/dashboard.html"

status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/applications.html" --write-out '%{http_code}' "$BASE_URL/pengajuan.php")"
[[ "$status" == "200" ]]
grep -Fq "$KRS_APPLICATION_SESSION_ID" "$WORK_DIR/applications.html"
token="$(csrf_from_file "$WORK_DIR/applications.html")"

status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/created.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=create' \
  --data-urlencode "id_krs=$KRS_APPLICATION_SESSION_ID" "$BASE_URL/pengajuan.php")"
[[ "$status" == "303" ]]

curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  "$BASE_URL/pengajuan.php" -o "$WORK_DIR/history.html"
application_id="$(extract_application_id "$WORK_DIR/history.html")"

status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/detail.html" --write-out '%{http_code}' "$BASE_URL/pengajuan.php?detail=$application_id")"
[[ "$status" == "200" ]]
grep -Fq 'Status Pengajuan' "$WORK_DIR/detail.html"
token="$(csrf_from_file "$WORK_DIR/detail.html")"

status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/course-added.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=add_course' \
  --data-urlencode "id_pengajuan=$application_id" \
  --data-urlencode 'id_jadwal=10' "$BASE_URL/pengajuan.php?detail=$application_id")"
[[ "$status" == "303" ]]

curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  "$BASE_URL/pengajuan.php?detail=$application_id" -o "$WORK_DIR/detail-with-course.html"
grep -Fq 'Statistika Bisnis' "$WORK_DIR/detail-with-course.html"
grep -Fq 'Total SKS: 3 / 24' "$WORK_DIR/detail-with-course.html"

# Jadwal 12 is the same course as schedule 10; it must be rejected.
status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/duplicate-course.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=add_course' \
  --data-urlencode "id_pengajuan=$application_id" \
  --data-urlencode 'id_jadwal=12' "$BASE_URL/pengajuan.php?detail=$application_id")"
[[ "$status" == "200" ]]
grep -Fq 'Mata kuliah tersebut sudah ada' "$WORK_DIR/duplicate-course.html"

# Test schedule 99 is a different course but overlaps schedule 10 on Monday.
status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/time-conflict.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=add_course' \
  --data-urlencode "id_pengajuan=$application_id" \
  --data-urlencode 'id_jadwal=99' "$BASE_URL/pengajuan.php?detail=$application_id")"
[[ "$status" == "200" ]]
grep -Fq 'Jadwal mata kuliah bertabrakan' "$WORK_DIR/time-conflict.html"

# Locking requires at least one course and moves the submission to Dosen PA review.
status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/locked.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=lock' \
  --data-urlencode "id_pengajuan=$application_id" "$BASE_URL/pengajuan.php?detail=$application_id")"
[[ "$status" == "303" ]]

status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/locked-detail.html" --write-out '%{http_code}' "$BASE_URL/pengajuan.php?detail=$application_id")"
[[ "$status" == "200" ]]
grep -Fq 'Menunggu Tinjauan' "$WORK_DIR/locked-detail.html"
grep -Fq 'hanya dapat dilihat' "$WORK_DIR/locked-detail.html"

# A second student cannot view the first student's application by changing its ID.
second_jar="$WORK_DIR/student-2.cookies"
login_student "KRS Application Other Student" "$second_jar"
status="$(curl --silent --show-error --cookie "$second_jar" --cookie-jar "$second_jar" \
  --output "$WORK_DIR/idor.html" --write-out '%{http_code}' "$BASE_URL/pengajuan.php?detail=$application_id")"
[[ "$status" == "404" ]]
grep -Fq 'Pengajuan KRS tidak ditemukan' "$WORK_DIR/idor.html"

# The second student can create and delete their own Draft application.
curl --silent --show-error --cookie "$second_jar" --cookie-jar "$second_jar" \
  "$BASE_URL/pengajuan.php" -o "$WORK_DIR/other-history.html"
token="$(csrf_from_file "$WORK_DIR/other-history.html")"
status="$(curl --silent --show-error --cookie "$second_jar" --cookie-jar "$second_jar" \
  --output "$WORK_DIR/other-created.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=create' \
  --data-urlencode "id_krs=$KRS_APPLICATION_SESSION_ID" "$BASE_URL/pengajuan.php")"
[[ "$status" == "303" ]]
curl --silent --show-error --cookie "$second_jar" --cookie-jar "$second_jar" \
  "$BASE_URL/pengajuan.php" -o "$WORK_DIR/other-history-created.html"
other_id="$(extract_application_id "$WORK_DIR/other-history-created.html")"
token="$(csrf_from_file "$WORK_DIR/other-history-created.html")"
status="$(curl --silent --show-error --cookie "$second_jar" --cookie-jar "$second_jar" \
  --output "$WORK_DIR/other-deleted.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=delete_request' \
  --data-urlencode "id_pengajuan=$other_id" "$BASE_URL/pengajuan.php")"
[[ "$status" == "303" ]]

echo "PASS: student application creation, course selection, duplicate and conflict rejection, 24-SKS limit page, locking, IDOR protection, and Draft deletion."
