#!/usr/bin/env bash
set -euo pipefail
: "\${KRS_PROFILE_PASSWORD:?CI must provide a temporary profile-test password}"
: "\${KRS_PROFILE_NEXT_PASSWORD:?CI must provide a temporary replacement password}"
BASE_URL="\${1:-http://127.0.0.1:8081}"
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
  curl --silent --show-error --cookie "$jar" --cookie-jar "$jar" "$BASE_URL/" -o "$page"
  token="$(csrf_from_file "$page")"
  status="$(curl --silent --show-error --cookie "$jar" --cookie-jar "$jar" \
    --output "$WORK_DIR/login-response.html" --write-out '%{http_code}' \
    --data-urlencode "csrf_token=$token" --data-urlencode "role=$role" \
    --data-urlencode "username=$username" --data-urlencode "password=$KRS_PROFILE_PASSWORD" "$BASE_URL/")"
  if [[ "$status" != "302" && "$status" != "303" ]]; then
    echo "Profile test login failed for role=$role (HTTP $status)" >&2
    exit 1
  fi
}

check_role_profile() {
  local role="$1" username="$2" expected="$3"
  local safe_role jar page status
  safe_role="$(echo "$role" | tr ' ' '_')"
  jar="$WORK_DIR/$safe_role.cookies"
  page="$WORK_DIR/$safe_role.html"
  login_account "$role" "$username" "$jar"
  status="$(curl --silent --show-error --cookie "$jar" --cookie-jar "$jar" \
    --output "$page" --write-out '%{http_code}' "$BASE_URL/profile.php")"
  [[ "$status" == "200" ]]
  grep -Fq 'Edit Profile Saya' "$page"
  grep -Fq 'ID Anda' "$page"
  grep -Fq "$expected" "$page"
}

status="$(curl --silent --show-error --output "$WORK_DIR/unauthorized.html" \
  --write-out '%{http_code}' "$BASE_URL/profile.php")"
[[ "$status" == "302" ]]

ADMIN_JAR="$WORK_DIR/admin.cookies"
login_account "Admin" "KRS Profile Admin" "$ADMIN_JAR"
status="$(curl --silent --show-error --cookie "$ADMIN_JAR" --cookie-jar "$ADMIN_JAR" \
  --output "$WORK_DIR/admin-profile.html" --write-out '%{http_code}' "$BASE_URL/profile.php")"
[[ "$status" == "200" ]]
grep -Fq 'KRS Profile Admin' "$WORK_DIR/admin-profile.html"
token="$(csrf_from_file "$WORK_DIR/admin-profile.html")"

status="$(curl --silent --show-error --cookie "$ADMIN_JAR" --cookie-jar "$ADMIN_JAR" \
  --output "$WORK_DIR/bad-csrf.html" --write-out '%{http_code}' \
  --data-urlencode 'csrf_token=invalid-token' --data-urlencode 'name=Should Not Save' "$BASE_URL/profile.php")"
[[ "$status" == "403" ]]

status="$(curl --silent --show-error --cookie "$ADMIN_JAR" --cookie-jar "$ADMIN_JAR" \
  --output "$WORK_DIR/saved.html" --write-out '%{http_code}' \
  --data-urlencode "csrf_token=$token" \
  --data-urlencode 'name=KRS Profile Admin Updated' \
  --data-urlencode "new_password=$KRS_PROFILE_NEXT_PASSWORD" \
  --data-urlencode "confirm_password=$KRS_PROFILE_NEXT_PASSWORD" "$BASE_URL/profile.php")"
[[ "$status" == "303" ]]

status="$(curl --silent --show-error --cookie "$ADMIN_JAR" --cookie-jar "$ADMIN_JAR" \
  --output "$WORK_DIR/updated-profile.html" --write-out '%{http_code}' "$BASE_URL/profile.php")"
[[ "$status" == "200" ]]
grep -Fq 'KRS Profile Admin Updated' "$WORK_DIR/updated-profile.html"

check_role_profile "Mahasiswa" "KRS Profile Student" "Angkatan"
grep -Fq 'Semester' "$WORK_DIR/Mahasiswa.html"
check_role_profile "Dosen" "KRS Profile Lecturer" "Posisi / Jabatan"
check_role_profile "Dosen PA" "KRS Profile Advisor" "Posisi / Jabatan"
check_role_profile "Kaprodi" "KRS Profile Head" "Posisi / Jabatan"

echo "PASS: profile authorization, CSRF, database update, password change, and role-specific profile fields."
