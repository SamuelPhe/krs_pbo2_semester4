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
        if tag.lower()=="input" and d.get("name")=="csrf_token": self.value=d.get("value")
p=P()
with open(sys.argv[1],encoding="utf-8") as f:p.feed(f.read())
if not p.value: raise SystemExit("CSRF not found")
print(p.value)
PY
}
row_id() {
  python3 - "$1" "$2" <<'PY'
import sys
from html.parser import HTMLParser
target=sys.argv[2]
class P(HTMLParser):
    cells=[]; current=False; answer=None
    def handle_starttag(self,tag,attrs):
        if tag.lower()=="tr": self.current=True; self.cells=[]
        elif tag.lower()=="td" and self.current: self.cells.append("")
    def handle_data(self,data):
        if self.current and self.cells: self.cells[-1]+=data.strip()
    def handle_endtag(self,tag):
        if tag.lower()=="tr" and self.current:
            if target in self.cells: self.answer=self.cells[0]
            self.current=False
p=P()
with open(sys.argv[1],encoding="utf-8") as f:p.feed(f.read())
if not p.answer: raise SystemExit("Could not find row for "+target)
print(p.answer)
PY
}
login() {
  local role="$1" username="$2" jar="$3" loginpage="$WORK/login.html" token code
  curl -fsS -c "$jar" "$BASE_URL/" -o "$loginpage"
  token="$(csrf "$loginpage")"
  code="$(curl -sS -b "$jar" -c "$jar" -o "$WORK/login-result.html" -w '%{http_code}' \
    --data-urlencode "csrf_token=$token" --data-urlencode "role=$role" \
    --data-urlencode "username=$username" --data-urlencode "password=$KRS_REMAINING_PASSWORD" "$BASE_URL/")"
  [[ "$code" == "302" || "$code" == "303" ]] || { echo "Login failed $role/$username: $code" >&2; exit 1; }
}
get_page() { curl -sS -b "$2" "$BASE_URL/$1" -o "$3"; }
post_form() {
  local jar="$1" path="$2" out="$3"; shift 3
  curl -sS -b "$jar" -c "$jar" -o "$out" -w '%{http_code}' "$BASE_URL/$path" "$@"
}
code_expected() {
  local actual="$1" expected="$2" label="$3"
  [[ "$actual" == "$expected" ]] || { echo "$label: expected HTTP $expected, got $actual" >&2; exit 1; }
}
admin="$WORK/admin.cookies"
login "Admin" "KRS Remaining Admin" "$admin"

# All Admin feature cards must now be real links.
get_page "" "$admin" "$WORK/dashboard.html"
for link in 'href="master-user.php"' 'href="master-data.php"' 'href="riwayat.php"' 'href="krs.php"'; do grep -Fq "$link" "$WORK/dashboard.html"; done

# Master User: create and edit a Dosen account.
get_page "master-user.php?type=dosen&new=1" "$admin" "$WORK/dosen-new.html"
token="$(csrf "$WORK/dosen-new.html")"
status="$(post_form "$admin" "master-user.php?type=dosen" "$WORK/dosen-created.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=dosen' --data-urlencode 'edit_id=0' \
  --data-urlencode 'action=save' --data-urlencode 'id=' --data-urlencode 'name=KRS CRUD Lecturer' \
  --data-urlencode 'position=dosen' --data-urlencode "password=$KRS_REMAINING_PASSWORD")"
code_expected "$status" 303 "Create Dosen"
get_page "master-user.php?type=dosen" "$admin" "$WORK/dosen-list.html"
lecturer_id="$(row_id "$WORK/dosen-list.html" "KRS CRUD Lecturer")"

get_page "master-user.php?type=dosen&edit=$lecturer_id" "$admin" "$WORK/dosen-edit.html"
token="$(csrf "$WORK/dosen-edit.html")"
status="$(post_form "$admin" "master-user.php?type=dosen" "$WORK/dosen-updated.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=dosen' --data-urlencode "edit_id=$lecturer_id" \
  --data-urlencode 'action=save' --data-urlencode "id=$lecturer_id" --data-urlencode 'name=KRS CRUD Lecturer Updated' \
  --data-urlencode 'position=dosen' --data-urlencode 'password=')"
code_expected "$status" 303 "Edit Dosen"
get_page "master-user.php?type=dosen" "$admin" "$WORK/dosen-list-updated.html"
grep -Fq 'KRS CRUD Lecturer Updated' "$WORK/dosen-list-updated.html"

# Create a Kaprodi account, used by the Master Data program form.
get_page "master-user.php?type=dosen&new=1" "$admin" "$WORK/kaprodi-new.html"
token="$(csrf "$WORK/kaprodi-new.html")"
status="$(post_form "$admin" "master-user.php?type=dosen" "$WORK/kaprodi-created.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=dosen' --data-urlencode 'edit_id=0' \
  --data-urlencode 'action=save' --data-urlencode 'id=' --data-urlencode 'name=KRS CRUD Kaprodi' \
  --data-urlencode 'position=kaprodi' --data-urlencode "password=$KRS_REMAINING_PASSWORD")"
code_expected "$status" 303 "Create Kaprodi"
get_page "master-user.php?type=dosen" "$admin" "$WORK/dosen-list-2.html"
kaprodi_id="$(row_id "$WORK/dosen-list-2.html" "KRS CRUD Kaprodi")"

# Create and edit a student, including Prodi and Dosen PA assignment.
get_page "master-user.php?type=mahasiswa&new=1" "$admin" "$WORK/student-new.html"
token="$(csrf "$WORK/student-new.html")"
status="$(post_form "$admin" "master-user.php?type=mahasiswa" "$WORK/student-created.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=mahasiswa' --data-urlencode 'edit_id=0' \
  --data-urlencode 'action=save' --data-urlencode 'id=' --data-urlencode 'name=KRS CRUD Student' \
  --data-urlencode 'program=111' --data-urlencode 'advisor=995002' \
  --data-urlencode 'cohort=2026' --data-urlencode 'semester=3' \
  --data-urlencode "password=$KRS_REMAINING_PASSWORD")"
code_expected "$status" 303 "Create student"
get_page "master-user.php?type=mahasiswa" "$admin" "$WORK/student-list.html"
student_id="$(row_id "$WORK/student-list.html" "KRS CRUD Student")"
get_page "master-user.php?type=mahasiswa&edit=$student_id" "$admin" "$WORK/student-edit.html"
token="$(csrf "$WORK/student-edit.html")"
status="$(post_form "$admin" "master-user.php?type=mahasiswa" "$WORK/student-updated.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=mahasiswa' --data-urlencode "edit_id=$student_id" \
  --data-urlencode 'action=save' --data-urlencode "id=$student_id" --data-urlencode 'name=KRS CRUD Student Updated' \
  --data-urlencode 'program=111' --data-urlencode 'advisor=995002' \
  --data-urlencode 'cohort=2026' --data-urlencode 'semester=4' --data-urlencode 'password=')"
code_expected "$status" 303 "Edit student"
get_page "master-user.php?type=mahasiswa" "$admin" "$WORK/student-list-updated.html"
grep -Fq 'KRS CRUD Student Updated' "$WORK/student-list-updated.html"

# A student with existing KRS submissions must not be deleted.
token="$(csrf "$WORK/student-list-updated.html")"
status="$(post_form "$admin" "master-user.php?type=mahasiswa" "$WORK/student-delete-guard.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=mahasiswa' \
  --data-urlencode 'id=995001' --data-urlencode 'action=delete')"
code_expected "$status" 200 "Student delete guard"
grep -Fq 'sudah memiliki riwayat pengajuan KRS' "$WORK/student-delete-guard.html"

# Self-delete protection for the logged-in Admin.
get_page "master-user.php?type=admin" "$admin" "$WORK/admin-list.html"
token="$(csrf "$WORK/admin-list.html")"
status="$(post_form "$admin" "master-user.php?type=admin" "$WORK/admin-self-delete.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=admin' \
  --data-urlencode 'id=995001' --data-urlencode 'action=delete')"
code_expected "$status" 200 "Self delete guard"
grep -Fq 'tidak boleh dihapus' "$WORK/admin-self-delete.html"

# Create and remove an unrelated temporary Admin account.
get_page "master-user.php?type=admin&new=1" "$admin" "$WORK/admin-new.html"
token="$(csrf "$WORK/admin-new.html")"
status="$(post_form "$admin" "master-user.php?type=admin" "$WORK/admin-created.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=admin' --data-urlencode 'edit_id=0' \
  --data-urlencode 'action=save' --data-urlencode 'id=' --data-urlencode 'name=KRS CRUD Temporary Admin' \
  --data-urlencode "password=$KRS_REMAINING_PASSWORD")"
code_expected "$status" 303 "Create Admin"
get_page "master-user.php?type=admin" "$admin" "$WORK/admin-list-2.html"
temporary_admin_id="$(row_id "$WORK/admin-list-2.html" "KRS CRUD Temporary Admin")"
token="$(csrf "$WORK/admin-list-2.html")"
status="$(post_form "$admin" "master-user.php?type=admin" "$WORK/admin-deleted.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=admin' \
  --data-urlencode "id=$temporary_admin_id" --data-urlencode 'action=delete')"
code_expected "$status" 303 "Delete Admin"

# Master Data: create a program and assign its Kaprodi.
get_page "master-data.php?type=prodi&new=1" "$admin" "$WORK/prodi-new.html"
token="$(csrf "$WORK/prodi-new.html")"
status="$(post_form "$admin" "master-data.php?type=prodi" "$WORK/prodi-created.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=prodi' --data-urlencode 'edit_id=0' \
  --data-urlencode 'action=save' --data-urlencode 'id=' --data-urlencode 'name=KRS CRUD Program' \
  --data-urlencode "kaprodi=$kaprodi_id")"
code_expected "$status" 303 "Create program"
get_page "master-data.php?type=prodi" "$admin" "$WORK/prodi-list.html"
program_id="$(row_id "$WORK/prodi-list.html" "KRS CRUD Program")"
get_page "master-data.php?type=prodi&edit=$program_id" "$admin" "$WORK/prodi-edit.html"
token="$(csrf "$WORK/prodi-edit.html")"
status="$(post_form "$admin" "master-data.php?type=prodi" "$WORK/prodi-updated.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=prodi' --data-urlencode "edit_id=$program_id" \
  --data-urlencode 'action=save' --data-urlencode "id=$program_id" --data-urlencode 'name=KRS CRUD Program Updated' \
  --data-urlencode "kaprodi=$kaprodi_id")"
code_expected "$status" 303 "Edit program"

# Create a course, class, room, and an actual schedule row.
get_page "master-data.php?type=matkul&new=1" "$admin" "$WORK/course-new.html"
token="$(csrf "$WORK/course-new.html")"
status="$(post_form "$admin" "master-data.php?type=matkul" "$WORK/course-created.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=matkul' --data-urlencode 'edit_id=0' \
  --data-urlencode 'action=save' --data-urlencode 'id=' --data-urlencode 'name=KRS CRUD Course' \
  --data-urlencode 'program=111' --data-urlencode 'sks=3' --data-urlencode 'semester=3')"
code_expected "$status" 303 "Create course"
get_page "master-data.php?type=matkul" "$admin" "$WORK/course-list.html"
course_id="$(row_id "$WORK/course-list.html" "KRS CRUD Course")"

get_page "master-data.php?type=kelas&new=1" "$admin" "$WORK/class-new.html"
token="$(csrf "$WORK/class-new.html")"
status="$(post_form "$admin" "master-data.php?type=kelas" "$WORK/class-created.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=kelas' --data-urlencode 'edit_id=0' \
  --data-urlencode 'action=save' --data-urlencode 'id=' --data-urlencode 'name=KRS CRUD Class')"
code_expected "$status" 303 "Create class"
get_page "master-data.php?type=kelas" "$admin" "$WORK/class-list.html"
class_id="$(row_id "$WORK/class-list.html" "KRS CRUD Class")"

get_page "master-data.php?type=ruang&new=1" "$admin" "$WORK/room-new.html"
token="$(csrf "$WORK/room-new.html")"
status="$(post_form "$admin" "master-data.php?type=ruang" "$WORK/room-created.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=ruang' --data-urlencode 'edit_id=0' \
  --data-urlencode 'action=save' --data-urlencode 'id=' --data-urlencode 'name=KRS CRUD Room')"
code_expected "$status" 303 "Create room"
get_page "master-data.php?type=ruang" "$admin" "$WORK/room-list.html"
room_id="$(row_id "$WORK/room-list.html" "KRS CRUD Room")"

get_page "master-data.php?type=jadwal&new=1" "$admin" "$WORK/schedule-new.html"
token="$(csrf "$WORK/schedule-new.html")"
status="$(post_form "$admin" "master-data.php?type=jadwal" "$WORK/schedule-created.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=jadwal' --data-urlencode 'edit_id=0' \
  --data-urlencode 'action=save' --data-urlencode 'id=' --data-urlencode "course=$course_id" \
  --data-urlencode "lecturer=$lecturer_id" --data-urlencode 'day=sabtu' \
  --data-urlencode "class=$class_id" --data-urlencode "room=$room_id" \
  --data-urlencode 'start=10:00' --data-urlencode 'end=11:00')"
code_expected "$status" 303 "Create schedule"
get_page "master-data.php?type=jadwal" "$admin" "$WORK/schedule-list.html"
schedule_id="$(row_id "$WORK/schedule-list.html" "KRS CRUD Course")"

# Reject a resource/time overlap.
get_page "master-data.php?type=jadwal&new=1" "$admin" "$WORK/schedule-conflict-form.html"
token="$(csrf "$WORK/schedule-conflict-form.html")"
status="$(post_form "$admin" "master-data.php?type=jadwal" "$WORK/schedule-conflict.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=jadwal' --data-urlencode 'edit_id=0' \
  --data-urlencode 'action=save' --data-urlencode 'id=' --data-urlencode "course=$course_id" \
  --data-urlencode "lecturer=$lecturer_id" --data-urlencode 'day=sabtu' \
  --data-urlencode "class=$class_id" --data-urlencode "room=$room_id" \
  --data-urlencode 'start=10:30' --data-urlencode 'end=11:30')"
code_expected "$status" 200 "Schedule overlap validation"
grep -Fq 'Jadwal bentrok' "$WORK/schedule-conflict.html"

# Edit must update the same schedule instead of inserting a duplicate.
get_page "master-data.php?type=jadwal&edit=$schedule_id" "$admin" "$WORK/schedule-edit.html"
token="$(csrf "$WORK/schedule-edit.html")"
status="$(post_form "$admin" "master-data.php?type=jadwal" "$WORK/schedule-updated.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=jadwal' --data-urlencode "edit_id=$schedule_id" \
  --data-urlencode 'action=save' --data-urlencode "id=$schedule_id" --data-urlencode "course=$course_id" \
  --data-urlencode "lecturer=$lecturer_id" --data-urlencode 'day=sabtu' \
  --data-urlencode "class=$class_id" --data-urlencode "room=$room_id" \
  --data-urlencode 'start=10:15' --data-urlencode 'end=11:15')"
code_expected "$status" 303 "Edit schedule"
get_page "master-data.php?type=jadwal" "$admin" "$WORK/schedule-list-updated.html"
grep -Fq '10:15:00' "$WORK/schedule-list-updated.html"

# Referenced course cannot be deleted while its schedule still exists.
get_page "master-data.php?type=matkul" "$admin" "$WORK/course-list-2.html"
token="$(csrf "$WORK/course-list-2.html")"
status="$(post_form "$admin" "master-data.php?type=matkul" "$WORK/course-delete-guard.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=matkul' \
  --data-urlencode "id=$course_id" --data-urlencode 'action=delete')"
code_expected "$status" 200 "Course delete guard"
grep -Fq 'masih memiliki jadwal kuliah' "$WORK/course-delete-guard.html"

# Remove the disposable schedule and then its unreferenced supporting data.
get_page "master-data.php?type=jadwal" "$admin" "$WORK/schedule-list-3.html"
token="$(csrf "$WORK/schedule-list-3.html")"
status="$(post_form "$admin" "master-data.php?type=jadwal" "$WORK/schedule-deleted.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=jadwal' \
  --data-urlencode "id=$schedule_id" --data-urlencode 'action=delete')"
code_expected "$status" 303 "Delete schedule"

get_page "master-data.php?type=matkul" "$admin" "$WORK/course-list-3.html"
token="$(csrf "$WORK/course-list-3.html")"
status="$(post_form "$admin" "master-data.php?type=matkul" "$WORK/course-deleted.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=matkul' \
  --data-urlencode "id=$course_id" --data-urlencode 'action=delete')"
code_expected "$status" 303 "Delete course"

get_page "master-data.php?type=kelas" "$admin" "$WORK/class-list-2.html"
token="$(csrf "$WORK/class-list-2.html")"
status="$(post_form "$admin" "master-data.php?type=kelas" "$WORK/class-deleted.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=kelas' \
  --data-urlencode "id=$class_id" --data-urlencode 'action=delete')"
code_expected "$status" 303 "Delete class"

get_page "master-data.php?type=ruang" "$admin" "$WORK/room-list-2.html"
token="$(csrf "$WORK/room-list-2.html")"
status="$(post_form "$admin" "master-data.php?type=ruang" "$WORK/room-deleted.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=ruang' \
  --data-urlencode "id=$room_id" --data-urlencode 'action=delete')"
code_expected "$status" 303 "Delete room"

get_page "master-data.php?type=prodi" "$admin" "$WORK/prodi-list-2.html"
token="$(csrf "$WORK/prodi-list-2.html")"
status="$(post_form "$admin" "master-data.php?type=prodi" "$WORK/prodi-deleted.html" \
  --data-urlencode "csrf_token=$token" --data-urlencode 'type=prodi' \
  --data-urlencode "id=$program_id" --data-urlencode 'action=delete')"
code_expected "$status" 303 "Delete program"

# Archive filtering, read-only detail, CSV export, and print-to-PDF view.
current_year="$(date +%Y)"
get_page "riwayat.php?year=$current_year&semester=3" "$admin" "$WORK/history.html"
grep -Fq 'KRS Remaining Student' "$WORK/history.html"
get_page "riwayat.php?detail=995001&year=$current_year&semester=3" "$admin" "$WORK/history-detail.html"
grep -Fq 'KRS Remaining Course' "$WORK/history-detail.html"
status="$(curl -sS -b "$admin" -o "$WORK/history.csv" -w '%{http_code}' "$BASE_URL/riwayat.php?year=$current_year&semester=3&export=csv")"
code_expected "$status" 200 "CSV export"
grep -Fq 'ID Pengajuan,ID Sesi,ID Mahasiswa' "$WORK/history.csv"
get_page "riwayat.php?year=$current_year&semester=3&print=1" "$admin" "$WORK/history-print.html"
grep -Fq 'window.print()' "$WORK/history-print.html"

# Other roles cannot open Admin CRUD or archive pages.
student_jar="$WORK/student.cookies"
login "Mahasiswa" "KRS Remaining Student" "$student_jar"
for page in master-user.php master-data.php riwayat.php; do
  status="$(curl -sS -b "$student_jar" -o "$WORK/denied.html" -w '%{http_code}' "$BASE_URL/$page")"
  code_expected "$status" 403 "Student denied $page"
done

echo "PASS: Master User CRUD, Master Data CRUD and relationship guards, KRS archive filters/detail/CSV/print, and role restrictions."
