#!/usr/bin/env bash
set -eo pipefail
if [ -z "$KRS_PERIOD_TEST_PASSWORD" ]; then echo "Missing KRS period test password" >&2; exit 1; fi
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
    --data-urlencode "password=$KRS_PERIOD_TEST_PASSWORD" "$BASE_URL/")"
  if [[ "$status" != "302" && "$status" != "303" ]]; then
    echo "Login failed for role=$role (HTTP $status)" >&2
    exit 1
  fi
}

extract_period_id() {
  python3 - "$1" "$2" <<'PY'
import sys
from html.parser import HTMLParser
target = sys.argv[2]
class Rows(HTMLParser):
    current_id = None
    cells = []
    found = None
    def handle_starttag(self, tag, attrs):
        if tag.lower() == "tr":
            self.current_id = dict(attrs).get("data-id")
            self.cells = []
    def handle_data(self, data):
        if self.current_id is not None:
            self.cells.append(data.strip())
    def handle_endtag(self, tag):
        if tag.lower() == "tr" and self.current_id is not None:
            if target in self.cells:
                self.found = self.current_id
            self.current_id = None
parser = Rows()
with open(sys.argv[1], encoding="utf-8") as source:
    parser.feed(source.read())
if not parser.found:
    raise SystemExit("Could not find created KRS period row")
print(parser.found)
PY
}

admin_jar="$WORK_DIR/admin.cookies"
login_account "Admin" "KRS Period Admin" "$admin_jar"

status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/dashboard.html" --write-out '%{http_code}' "$BASE_URL/")"
[[ "$status" == "200" ]]
grep -Fq 'href="krs.php"' "$WORK_DIR/dashboard.html"

status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/new-period.html" --write-out '%{http_code}' "$BASE_URL/krs.php?new=1")"
[[ "$status" == "200" ]]
grep -Fq 'Manajemen Aktifasi Sesi KRS' "$WORK_DIR/new-period.html"
token="$(csrf_from_file "$WORK_DIR/new-period.html")"

status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/created.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=save' \
  --data-urlencode 'id_krs=' \
  --data-urlencode 'id_prodi=111' \
  --data-urlencode 'semester=5' \
  --data-urlencode 'tahun_ajaran=2091' \
  --data-urlencode 'status_krs=tertutup' \
  --data-urlencode 'tanggal_mulai=2091-01-01' \
  --data-urlencode 'tanggal_selesai=2091-01-15' "$BASE_URL/krs.php")"
[[ "$status" == "303" ]]

status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/list-created.html" --write-out '%{http_code}' "$BASE_URL/krs.php")"
[[ "$status" == "200" ]]
grep -Fq '2091' "$WORK_DIR/list-created.html"
period_id="$(extract_period_id "$WORK_DIR/list-created.html" "2091")"

status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/edit-form.html" --write-out '%{http_code}' "$BASE_URL/krs.php?edit=$period_id")"
[[ "$status" == "200" ]]
token="$(csrf_from_file "$WORK_DIR/edit-form.html")"

status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/updated.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=save' \
  --data-urlencode "id_krs=$period_id" \
  --data-urlencode 'id_prodi=111' \
  --data-urlencode 'semester=6' \
  --data-urlencode 'tahun_ajaran=2092' \
  --data-urlencode 'status_krs=tertutup' \
  --data-urlencode 'tanggal_mulai=2092-06-01' \
  --data-urlencode 'tanggal_selesai=2092-06-15' "$BASE_URL/krs.php")"
[[ "$status" == "303" ]]

status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/list-updated.html" --write-out '%{http_code}' "$BASE_URL/krs.php")"
[[ "$status" == "200" ]]
grep -Fq '2092' "$WORK_DIR/list-updated.html"

status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/overlap.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=save' \
  --data-urlencode 'id_krs=' \
  --data-urlencode 'id_prodi=111' \
  --data-urlencode 'semester=6' \
  --data-urlencode 'tahun_ajaran=2092' \
  --data-urlencode 'status_krs=tertutup' \
  --data-urlencode 'tanggal_mulai=2092-06-10' \
  --data-urlencode 'tanggal_selesai=2092-06-20' "$BASE_URL/krs.php")"
[[ "$status" == "200" ]]
grep -Fq 'Tanggal sesi beririsan' "$WORK_DIR/overlap.html"

# The historical SQL dump has two submissions linked to KRS session 11.
status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/delete-guard.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=delete' \
  --data-urlencode 'id_krs=11' "$BASE_URL/krs.php")"
[[ "$status" == "200" ]]
grep -Fq 'Sesi tidak dapat dihapus' "$WORK_DIR/delete-guard.html"

# A session without submissions can be deleted.
status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/new-period-2.html" --write-out '%{http_code}' "$BASE_URL/krs.php?new=1")"
[[ "$status" == "200" ]]
token="$(csrf_from_file "$WORK_DIR/new-period-2.html")"
status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/created-2.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=save' \
  --data-urlencode 'id_krs=' \
  --data-urlencode 'id_prodi=122' \
  --data-urlencode 'semester=3' \
  --data-urlencode 'tahun_ajaran=2093' \
  --data-urlencode 'status_krs=tertutup' \
  --data-urlencode 'tanggal_mulai=2093-02-01' \
  --data-urlencode 'tanggal_selesai=2093-02-15' "$BASE_URL/krs.php")"
[[ "$status" == "303" ]]
curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" "$BASE_URL/krs.php" -o "$WORK_DIR/list-second.html"
delete_id="$(extract_period_id "$WORK_DIR/list-second.html" "2093")"
token="$(csrf_from_file "$WORK_DIR/list-second.html")"
status="$(curl --silent --show-error --cookie "$admin_jar" --cookie-jar "$admin_jar" \
  --output "$WORK_DIR/deleted.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'action=delete' \
  --data-urlencode "id_krs=$delete_id" "$BASE_URL/krs.php")"
[[ "$status" == "303" ]]

# A student must not be able to open the admin session-management route.
student_jar="$WORK_DIR/student.cookies"
login_account "Mahasiswa" "KRS Period Student" "$student_jar"
status="$(curl --silent --show-error --cookie "$student_jar" --cookie-jar "$student_jar" \
  --output "$WORK_DIR/student-denied.html" --write-out '%{http_code}' "$BASE_URL/krs.php")"
[[ "$status" == "403" ]]

echo "PASS: admin session CRUD, overlap validation, linked-submission delete guard, and student access denial."
