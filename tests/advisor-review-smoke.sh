#!/usr/bin/env bash
set -eo pipefail
if [ -z "$KRS_REVIEW_TEST_PASSWORD" ]; then echo "Missing KRS review test password" >&2; exit 1; fi
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

login_account() {
  local role="$1" username="$2" jar="$3"
  local page="$WORK_DIR/login.html" token status
  curl --silent --show-error --cookie-jar "$jar" "$BASE_URL/" -o "$page"
  token="$(csrf_from_file "$page")"
  status="$(curl --silent --show-error --cookie "$jar" --cookie-jar "$jar" \
    --output "$WORK_DIR/login-response.html" --write-out '%{http_code}' \
    --data-urlencode "csrf_token=$token" \
    --data-urlencode "role=$role" \
    --data-urlencode "username=$username" \
    --data-urlencode "password=$KRS_REVIEW_TEST_PASSWORD" "$BASE_URL/")"
  if [[ "$status" != "302" && "$status" != "303" ]]; then
    echo "Login failed for role=$role username=$username (HTTP $status)" >&2
    exit 1
  fi
}

advisor_jar="$WORK_DIR/advisor.cookies"
login_account "Dosen PA" "KRS Review Advisor" "$advisor_jar"

status="$(curl --silent --show-error --cookie "$advisor_jar" --cookie-jar "$advisor_jar" \
  --output "$WORK_DIR/dashboard.html" --write-out '%{http_code}' "$BASE_URL/")"
[[ "$status" == "200" ]]
grep -Fq 'href="review.php"' "$WORK_DIR/dashboard.html"

status="$(curl --silent --show-error --cookie "$advisor_jar" --cookie-jar "$advisor_jar" \
  --output "$WORK_DIR/queue.html" --write-out '%{http_code}' "$BASE_URL/review.php")"
[[ "$status" == "200" ]]
grep -Fq 'KRS Review Student' "$WORK_DIR/queue.html"
if grep -Fq 'KRS Review Other Student' "$WORK_DIR/queue.html"; then
  echo "Advisor queue leaked a student assigned to another advisor" >&2
  exit 1
fi

# A different advisor's application must not be accessible by changing its ID.
status="$(curl --silent --show-error --cookie "$advisor_jar" --cookie-jar "$advisor_jar" \
  --output "$WORK_DIR/foreign-application.html" --write-out '%{http_code}' \
  "$BASE_URL/review.php?id=993002")"
[[ "$status" == "404" ]]

status="$(curl --silent --show-error --cookie "$advisor_jar" --cookie-jar "$advisor_jar" \
  --output "$WORK_DIR/application.html" --write-out '%{http_code}' \
  "$BASE_URL/review.php?id=993001")"
[[ "$status" == "200" ]]
grep -Fq 'Statistika Bisnis' "$WORK_DIR/application.html"
grep -Fq 'Struktur Data' "$WORK_DIR/application.html"
token="$(csrf_from_file "$WORK_DIR/application.html")"

# The application detail endpoint rejects forged CSRF tokens.
status="$(curl --silent --show-error --cookie "$advisor_jar" --cookie-jar "$advisor_jar" \
  --output "$WORK_DIR/bad-csrf.html" --write-out '%{http_code}' \
  --data-urlencode 'csrf_token=invalid-token' \
  --data-urlencode 'action=approve_selected' \
  --data-urlencode 'id_pengajuan=993001' \
  --data-urlencode 'detail_ids[]=993001' "$BASE_URL/review.php?id=993001")"
[[ "$status" == "403" ]]

# Approve the first course item.
status="$(curl --silent --show-error --cookie "$advisor_jar" --cookie-jar "$advisor_jar" \
  --output "$WORK_DIR/approved.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=approve_selected' \
  --data-urlencode 'id_pengajuan=993001' \
  --data-urlencode 'detail_ids[]=993001' "$BASE_URL/review.php?id=993001")"
[[ "$status" == "303" ]]

# Reject the second item and store the explanation against that course.
curl --silent --show-error --cookie "$advisor_jar" --cookie-jar "$advisor_jar" \
  "$BASE_URL/review.php?id=993001" -o "$WORK_DIR/after-approve.html"
token="$(csrf_from_file "$WORK_DIR/after-approve.html")"
status="$(curl --silent --show-error --cookie "$advisor_jar" --cookie-jar "$advisor_jar" \
  --output "$WORK_DIR/rejected.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=reject_selected' \
  --data-urlencode 'id_pengajuan=993001' \
  --data-urlencode 'detail_ids[]=993002' \
  --data-urlencode 'catatan_revisi=Ganti jadwal kuliah karena tidak sesuai rencana studi.' \
  "$BASE_URL/review.php?id=993001")"
[[ "$status" == "303" ]]

# Complete review: a rejected course sends the overall application back to the student.
curl --silent --show-error --cookie "$advisor_jar" --cookie-jar "$advisor_jar" \
  "$BASE_URL/review.php?id=993001" -o "$WORK_DIR/ready-to-finish.html"
grep -Fq 'Selesaikan Review' "$WORK_DIR/ready-to-finish.html"
token="$(csrf_from_file "$WORK_DIR/ready-to-finish.html")"
status="$(curl --silent --show-error --cookie "$advisor_jar" --cookie-jar "$advisor_jar" \
  --output "$WORK_DIR/finished.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=finish_review' \
  --data-urlencode 'id_pengajuan=993001' "$BASE_URL/review.php?id=993001")"
[[ "$status" == "303" ]]

# The student can see the advisor's explanation after the application is returned.
student_jar="$WORK_DIR/student.cookies"
login_account "Mahasiswa" "KRS Review Student" "$student_jar"
status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/student-result.html" --write-out '%{http_code}' \
  "$BASE_URL/pengajuan.php?detail=993001")"
[[ "$status" == "200" ]]
grep -Fq 'Perlu Revisi' "$WORK_DIR/student-result.html"
grep -Fq 'Ganti jadwal kuliah karena tidak sesuai rencana studi.' "$WORK_DIR/student-result.html"

# A second advisor can only review their own student and pass a fully approved form to Kaprodi.
other_advisor_jar="$WORK_DIR/other-advisor.cookies"
login_account "Dosen PA" "KRS Review Other Advisor" "$other_advisor_jar"
curl --silent --show-error --cookie "$other_advisor_jar" --cookie-jar "$other_advisor_jar" \
  "$BASE_URL/review.php" -o "$WORK_DIR/other-queue.html"
grep -Fq 'KRS Review Other Student' "$WORK_DIR/other-queue.html"
if grep -Fq 'KRS Review Student' "$WORK_DIR/other-queue.html"; then
  echo "Second advisor queue contains another advisor's student" >&2
  exit 1
fi
curl --silent --show-error --cookie "$other_advisor_jar" --cookie-jar "$other_advisor_jar" \
  "$BASE_URL/review.php?id=993002" -o "$WORK_DIR/other-application.html"
token="$(csrf_from_file "$WORK_DIR/other-application.html")"
status="$(curl --silent --show-error --cookie "$other_advisor_jar" --cookie-jar "$other_advisor_jar" \
  --output "$WORK_DIR/other-approved.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=approve_selected' \
  --data-urlencode 'id_pengajuan=993002' \
  --data-urlencode 'detail_ids[]=993003' "$BASE_URL/review.php?id=993002")"
[[ "$status" == "303" ]]
curl --silent --show-error --cookie "$other_advisor_jar" --cookie-jar "$other_advisor_jar" \
  "$BASE_URL/review.php?id=993002" -o "$WORK_DIR/other-ready.html"
token="$(csrf_from_file "$WORK_DIR/other-ready.html")"
status="$(curl --silent --show-error --cookie "$other_advisor_jar" --cookie-jar "$other_advisor_jar" \
  --output "$WORK_DIR/other-finished.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=finish_review' \
  --data-urlencode 'id_pengajuan=993002' "$BASE_URL/review.php?id=993002")"
[[ "$status" == "303" ]]

# A lecturer without Dosen PA/Kaprodi position must not enter the review route.
lecturer_jar="$WORK_DIR/lecturer.cookies"
login_account "Dosen" "KRS Review Lecturer" "$lecturer_jar"
status="$(curl --silent --show-error --cookie "$lecturer_jar" --cookie-jar "$lecturer_jar" \
  --output "$WORK_DIR/lecturer-denied.html" --write-out '%{http_code}' "$BASE_URL/review.php")"
[[ "$status" == "403" ]]

echo "PASS: advisor queue ownership, IDOR protection, CSRF, approval, revision notes, final review status, student feedback, and advisor-role authorization."
