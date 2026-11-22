#!/usr/bin/env bash
set -euo pipefail
: "$KRS_REMAINING_PASSWORD"
BASE_URL="http://127.0.0.1:8081"
if [ "$#" -gt 0 ]; then BASE_URL="$1"; fi
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

csrf() {
  python3 - "$1" <<'PY'
import sys
from html.parser import HTMLParser
class P(HTMLParser):
    value=None
    def handle_starttag(self,tag,attrs):
        d=dict(attrs)
        if tag.lower()=="input" and d.get("name")=="csrf_token":self.value=d.get("value")
p=P()
with open(sys.argv[1],encoding="utf-8") as f:p.feed(f.read())
if not p.value:raise SystemExit("CSRF not found")
print(p.value)
PY
}
login() {
  local role="$1" username="$2" jar="$3" page="$WORK/login.html" token code
  curl -fsS -c "$jar" "$BASE_URL/" -o "$page"
  token="$(csrf "$page")"
  code="$(curl -sS -b "$jar" -c "$jar" -o "$WORK/login-response.html" -w '%{http_code}' \
   --data-urlencode "csrf_token=$token" --data-urlencode "role=$role" \
   --data-urlencode "username=$username" --data-urlencode "password=$KRS_REMAINING_PASSWORD" "$BASE_URL/")"
  [[ "$code" == "302" || "$code" == "303" ]] || { echo "Login failed: $role/$username ($code)" >&2; exit 1; }
}
get_page(){ curl -sS -b "$2" "$BASE_URL/$1" -o "$3"; }
post(){
  local jar="$1" path="$2" out="$3";shift 3
  curl -sS -b "$jar" -c "$jar" -o "$out" -w '%{http_code}' "$BASE_URL/$path" "$@"
}
expect(){ [[ "$1" == "$2" ]] || { echo "$3 expected HTTP $2, got $1" >&2; exit 1; }; }

kaprodi="$WORK/kaprodi.cookies"
login "Kaprodi" "KRS Remaining Kaprodi" "$kaprodi"
get_page "" "$kaprodi" "$WORK/kaprodi-dashboard.html"
grep -Fq 'href="finalisasi.php"' "$WORK/kaprodi-dashboard.html"
grep -Fq 'href="mahasiswa-pa.php"' "$WORK/kaprodi-dashboard.html"
grep -Fq 'href="jadwal-ajar.php"' "$WORK/kaprodi-dashboard.html"

# A Kaprodi assigned to another program cannot open this application's finalization detail.
other_kaprodi="$WORK/other-kaprodi.cookies"
login "Kaprodi" "KRS Other Kaprodi" "$other_kaprodi"
status="$(curl -sS -b "$other_kaprodi" -o "$WORK/foreign-finalisasi.html" -w '%{http_code}' "$BASE_URL/finalisasi.php?id=995001")"
expect "$status" 404 "Foreign finalization detail"
grep -Fq 'tidak ditemukan pada program studi' "$WORK/foreign-finalisasi.html"

# Kaprodi approves all courses of one submission and finalizes it.
get_page "finalisasi.php" "$kaprodi" "$WORK/finalization-queue.html"
grep -Fq 'KRS Remaining Student' "$WORK/finalization-queue.html"
grep -Fq 'KRS Remaining Second Student' "$WORK/finalization-queue.html"
status="$(curl -sS -b "$kaprodi" -o "$WORK/finalization-detail.html" -w '%{http_code}' "$BASE_URL/finalisasi.php?id=995001")"
expect "$status" 200 "Finalization detail"
grep -Fq 'KRS Remaining Course' "$WORK/finalization-detail.html"
token="$(csrf "$WORK/finalization-detail.html")"
status="$(post "$kaprodi" "finalisasi.php?id=995001" "$WORK/finalization-approved.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'action=approve_selected' \
  --data-urlencode 'id_pengajuan=995001' --data-urlencode 'detail_ids[]=995001')"
expect "$status" 303 "Approve course at Kaprodi stage"
get_page "finalisasi.php?id=995001" "$kaprodi" "$WORK/finalization-ready.html"
grep -Fq 'Selesaikan Finalisasi' "$WORK/finalization-ready.html"
token="$(csrf "$WORK/finalization-ready.html")"
status="$(post "$kaprodi" "finalisasi.php?id=995001" "$WORK/finalization-completed.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'action=finish' --data-urlencode 'id_pengajuan=995001')"
expect "$status" 303 "Complete finalization"

# Kaprodi can return a selected course for revision and finish with Ditolak.
get_page "finalisasi.php?id=995002" "$kaprodi" "$WORK/finalization-second.html"
token="$(csrf "$WORK/finalization-second.html")"
status="$(post "$kaprodi" "finalisasi.php?id=995002" "$WORK/finalization-rejected.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'action=reject_selected' \
  --data-urlencode 'id_pengajuan=995002' --data-urlencode 'detail_ids[]=995002' \
  --data-urlencode 'catatan_revisi=Perlu penyesuaian pilihan mata kuliah pada tahap finalisasi.')"
expect "$status" 303 "Return course for revision"
get_page "finalisasi.php?id=995002" "$kaprodi" "$WORK/finalization-second-ready.html"
token="$(csrf "$WORK/finalization-second-ready.html")"
status="$(post "$kaprodi" "finalisasi.php?id=995002" "$WORK/finalization-second-complete.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'action=finish' --data-urlencode 'id_pengajuan=995002')"
expect "$status" 303 "Complete rejected finalization"

# Student portal reflects final status and revision notes.
student="$WORK/student.cookies"
login "Mahasiswa" "KRS Remaining Student" "$student"
get_page "pengajuan.php?detail=995001" "$student" "$WORK/student-approved.html"
grep -Fq 'Disetujui' "$WORK/student-approved.html"

second_student="$WORK/second-student.cookies"
login "Mahasiswa" "KRS Remaining Second Student" "$second_student"
get_page "pengajuan.php?detail=995002" "$second_student" "$WORK/student-revision.html"
grep -Fq 'Perlu Revisi' "$WORK/student-revision.html"
grep -Fq 'Perlu penyesuaian pilihan mata kuliah pada tahap finalisasi.' "$WORK/student-revision.html"

# Dosen PA sees only students assigned to their own account and can open their archive.
advisor="$WORK/advisor.cookies"
login "Dosen PA" "KRS Remaining Advisor" "$advisor"
get_page "" "$advisor" "$WORK/advisor-dashboard.html"
grep -Fq 'href="mahasiswa-pa.php"' "$WORK/advisor-dashboard.html"
get_page "mahasiswa-pa.php" "$advisor" "$WORK/advisor-students.html"
grep -Fq 'KRS Remaining Student' "$WORK/advisor-students.html"
grep -Fq 'KRS Remaining Second Student' "$WORK/advisor-students.html"
if grep -Fq 'KRS Other Assigned Student' "$WORK/advisor-students.html"; then
  echo "Advisor list leaked a student assigned to another advisor" >&2; exit 1
fi
status="$(curl -sS -b "$advisor" -o "$WORK/advisor-idor.html" -w '%{http_code}' "$BASE_URL/mahasiswa-pa.php?student=995003")"
expect "$status" 404 "Advisor student IDOR guard"
get_page "mahasiswa-pa.php?student=995001&detail=995001" "$advisor" "$WORK/advisor-student-detail.html"
grep -Fq 'KRS Remaining Course' "$WORK/advisor-student-detail.html"

# Lecturer sees only their own teaching schedule and its eligible student roster.
lecturer="$WORK/lecturer.cookies"
login "Dosen" "KRS Remaining Lecturer" "$lecturer"
get_page "jadwal-ajar.php" "$lecturer" "$WORK/lecturer-schedules.html"
grep -Fq 'KRS Remaining Course' "$WORK/lecturer-schedules.html"
if grep -Fq 'KRS CRUD Course' "$WORK/lecturer-schedules.html"; then
  echo "Lecturer schedule list leaked another lecturer's class" >&2; exit 1
fi
get_page "jadwal-ajar.php?schedule=995001" "$lecturer" "$WORK/lecturer-roster.html"
grep -Fq 'KRS Remaining Student' "$WORK/lecturer-roster.html"
if grep -Fq 'KRS Remaining Second Student' "$WORK/lecturer-roster.html"; then
  echo "A returned KRS should not count as an active roster member" >&2; exit 1
fi
status="$(curl -sS -b "$lecturer" -o "$WORK/foreign-schedule.html" -w '%{http_code}' "$BASE_URL/jadwal-ajar.php?schedule=10")"
expect "$status" 404 "Foreign schedule detail"

# The ordinary Dosen role must not access Dosen PA or Kaprodi-only screens.
status="$(curl -sS -b "$lecturer" -o "$WORK/lecturer-pa-denied.html" -w '%{http_code}' "$BASE_URL/mahasiswa-pa.php")"
expect "$status" 403 "Dosen denied Mahasiswa PA"
status="$(curl -sS -b "$lecturer" -o "$WORK/lecturer-final-denied.html" -w '%{http_code}' "$BASE_URL/finalisasi.php")"
expect "$status" 403 "Dosen denied Finalisasi"

echo "PASS: Kaprodi finalization/return, student status reflection, advisor student scope, lecturer schedule and roster scope, and role restrictions."
