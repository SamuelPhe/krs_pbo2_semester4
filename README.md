# KRS Academic Management System

Aplikasi desktop untuk membantu pengelolaan Kartu Rencana Studi (KRS) sebagai proyek perkuliahan Pemrograman Berorientasi Objek Semester 4.

## Fitur proyek
- Login dengan peran Mahasiswa, Dosen, dan Admin.
- Pengelolaan data program studi, mahasiswa, dosen, mata kuliah, kelas, ruang, dan jadwal.
- Pengajuan KRS dan pemilihan mata kuliah.
- Pemeriksaan mata kuliah ganda dan bentrok jadwal.
- Peninjauan/persetujuan KRS oleh Dosen PA.
- Finalisasi KRS oleh Kaprodi.
- Arsip KRS dan pencetakan laporan menggunakan JasperReports.

## Teknologi
Java 8, Java Swing, MySQL/JDBC, Maven, JasperReports, AbsoluteLayout, dan NetBeans GUI Builder.

## Preview antarmuka

**Halaman utama**

![Preview halaman utama](src/main/resources/desain/home%20page%20%282%29.png)

**Halaman login**

![Preview halaman login](src/main/resources/desain/Login%20%282%29.png)

**Detail KRS**

![Preview detail KRS](src/main/resources/desain/Detail%20KRS.png)

## Menjalankan proyek
1. Instal JDK 17 (disarankan untuk build), Maven, dan MySQL/MariaDB. Konfigurasi compiler pada `pom.xml` tetap menargetkan kompatibilitas bytecode Java 8.
2. Siapkan database `krs_db` beserta tabel sesuai skema database proyek.
3. Atur environment variable:
   - `DB_URL` (opsional; default `jdbc:mysql://127.0.0.1:3306/krs_db?serverTimezone=UTC`)
   - `DB_USER` (wajib)
   - `DB_PASSWORD` (wajib)
4. Jalankan `mvn clean compile`.
5. Jalankan `mvn exec:java` untuk membuka halaman login.

**Catatan:** repository belum menyertakan dump SQL database. Skema perlu disiapkan dari salinan database perkuliahan sebelum aplikasi dapat dijalankan.

## Keamanan
- Koneksi database menggunakan environment variable dan tidak lagi menyimpan password database di source.
- Password baru disimpan dengan PBKDF2-HMAC-SHA256 dan salt acak.
- Password lama berbentuk teks biasa akan diverifikasi saat login pertama, lalu otomatis ditingkatkan menjadi hash.
- Sebelum menggunakan versi ini, jalankan `database/password_hash_migration.sql` pada salinan database agar kolom password mendukung hash hingga 255 karakter.
- Ini proyek akademik dan belum dinyatakan siap produksi; lakukan pengujian pada database salinan.

## Konteks pengerjaan
Proyek dikerjakan sebagai tugas perkuliahan dengan panduan pembelajaran dan bantuan AI. Deskripsi fitur merangkum fungsi yang tampak pada kode dan struktur proyek.
