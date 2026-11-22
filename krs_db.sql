-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Waktu pembuatan: 21 Jun 2026 pada 15.15
-- Versi server: 10.4.32-MariaDB
-- Versi PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `krs_db`
--

-- --------------------------------------------------------

--
-- Struktur dari tabel `admin`
--

CREATE TABLE `admin` (
  `id_admin` int(10) NOT NULL,
  `nama_admin` varchar(50) NOT NULL,
  `password` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `admin`
--

INSERT INTO `admin` (`id_admin`, `nama_admin`, `password`) VALUES
(11, 'Admin1', '123');

-- --------------------------------------------------------

--
-- Struktur dari tabel `dosen`
--

CREATE TABLE `dosen` (
  `id_dosen` int(10) NOT NULL,
  `nama_dosen` varchar(50) NOT NULL,
  `posisi` enum('dosen','kaprodi','dosen pa') NOT NULL,
  `password` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `dosen`
--

INSERT INTO `dosen` (`id_dosen`, `nama_dosen`, `posisi`, `password`) VALUES
(10001, 'Dosen1', 'dosen', '123'),
(10002, 'Dosen2', 'dosen pa', '123'),
(10003, 'Dosen', 'dosen', '123'),
(10004, 'Kaprodi', 'kaprodi', '123'),
(100005, 'Dosenpa', 'dosen pa', '123');

-- --------------------------------------------------------

--
-- Struktur dari tabel `jadwal`
--

CREATE TABLE `jadwal` (
  `id_jadwal` int(10) NOT NULL,
  `id_matkul` int(10) NOT NULL,
  `id_dosen` int(10) NOT NULL,
  `hari` enum('senin','selasa','rabu','kamis','jumat','sabtu','minggu') NOT NULL,
  `kelas` varchar(10) NOT NULL,
  `jam_mulai` time NOT NULL,
  `jam_selesai` time NOT NULL,
  `ruang` varchar(5) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `jadwal`
--

INSERT INTO `jadwal` (`id_jadwal`, `id_matkul`, `id_dosen`, `hari`, `kelas`, `jam_mulai`, `jam_selesai`, `ruang`) VALUES
(9, 1001, 10002, 'rabu', '3', '16:00:00', '17:40:00', '4'),
(10, 2002, 10001, 'senin', '4', '16:00:00', '17:40:00', '4'),
(11, 1001, 10002, 'rabu', '1', '16:00:00', '17:40:00', '3'),
(12, 2002, 10001, 'rabu', '4', '16:00:00', '17:40:00', '5'),
(13, 1001, 10003, 'senin', '1', '07:00:00', '08:40:00', '3'),
(14, 2002, 10003, 'selasa', '1', '07:00:00', '08:40:00', '4'),
(15, 1002, 10004, 'rabu', '4', '07:00:00', '08:00:00', '4'),
(16, 1002, 10003, 'kamis', '1', '07:00:00', '09:00:00', '4');

-- --------------------------------------------------------

--
-- Struktur dari tabel `kelas`
--

CREATE TABLE `kelas` (
  `id_kelas` int(11) NOT NULL,
  `nama_kelas` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `kelas`
--

INSERT INTO `kelas` (`id_kelas`, `nama_kelas`) VALUES
(1, 'SB1'),
(3, 'BD3'),
(4, 'SB2');

-- --------------------------------------------------------

--
-- Struktur dari tabel `krs`
--

CREATE TABLE `krs` (
  `id_krs` int(6) NOT NULL,
  `id_prodi` int(6) NOT NULL,
  `id_dosen_pa` int(10) DEFAULT NULL,
  `semester` int(2) NOT NULL,
  `tahun_ajaran` int(4) NOT NULL,
  `status_krs` enum('terbuka','tertutup') NOT NULL,
  `tanggal_mulai` date NOT NULL,
  `tanggal_selesai` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `krs`
--

INSERT INTO `krs` (`id_krs`, `id_prodi`, `id_dosen_pa`, `semester`, `tahun_ajaran`, `status_krs`, `tanggal_mulai`, `tanggal_selesai`) VALUES
(9, 111, NULL, 2, 2026, 'tertutup', '2026-06-06', '2026-06-06'),
(10, 111, NULL, 4, 2026, 'tertutup', '2026-06-06', '2026-06-06'),
(11, 111, NULL, 2, 2026, 'terbuka', '2026-06-07', '2026-06-21');

-- --------------------------------------------------------

--
-- Struktur dari tabel `krs_detail`
--

CREATE TABLE `krs_detail` (
  `id_detail` int(10) NOT NULL,
  `id_pengajuan` int(10) NOT NULL,
  `id_jadwal` int(10) NOT NULL,
  `status_acc` enum('Disetujui','Ditinjau','Ditolak','AccDosenPA','Draft') DEFAULT 'Draft',
  `catatan_revisi` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `krs_detail`
--

INSERT INTO `krs_detail` (`id_detail`, `id_pengajuan`, `id_jadwal`, `status_acc`, `catatan_revisi`) VALUES
(28, 21, 16, 'Disetujui', ''),
(29, 21, 10, 'Disetujui', ''),
(34, 22, 14, 'Disetujui', ''),
(35, 22, 16, 'Disetujui', '');

-- --------------------------------------------------------

--
-- Struktur dari tabel `mahasiswa`
--

CREATE TABLE `mahasiswa` (
  `id_mahasiswa` int(10) NOT NULL,
  `id_prodi` int(10) NOT NULL,
  `id_dosen_pa` int(10) DEFAULT NULL,
  `nama_mahasiswa` varchar(50) NOT NULL,
  `angkatan` int(2) NOT NULL,
  `password` varchar(20) NOT NULL,
  `semester` int(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `mahasiswa`
--

INSERT INTO `mahasiswa` (`id_mahasiswa`, `id_prodi`, `id_dosen_pa`, `nama_mahasiswa`, `angkatan`, `password`, `semester`) VALUES
(1000, 111, 100005, 'Mahasiswa1', 2025, '123', 2),
(1004, 133, 10002, 'Davent', 2024, '123', 3),
(1005, 111, 10004, 'Samuel Wong', 2025, '123', 2),
(1006, 111, 10004, 'Mahasiswa2', 2026, '123', 1);

-- --------------------------------------------------------

--
-- Struktur dari tabel `matkul`
--

CREATE TABLE `matkul` (
  `id_matkul` int(10) NOT NULL,
  `id_prodi` int(10) NOT NULL,
  `nama_matkul` varchar(50) NOT NULL,
  `sks` int(2) NOT NULL,
  `semester` int(2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `matkul`
--

INSERT INTO `matkul` (`id_matkul`, `id_prodi`, `nama_matkul`, `sks`, `semester`) VALUES
(1001, 111, 'Pemrograman Berbasis Objek', 3, 4),
(1002, 111, 'Struktur Data', 3, 2),
(2002, 111, 'Statistika Bisnis', 3, 2);

-- --------------------------------------------------------

--
-- Struktur dari tabel `pengajuan_krs`
--

CREATE TABLE `pengajuan_krs` (
  `id_pengajuan` int(10) NOT NULL,
  `id_krs` int(10) NOT NULL,
  `id_mahasiswa` int(10) DEFAULT NULL,
  `id_dosen_acc` int(10) DEFAULT NULL,
  `tanggal_pengajuan` date DEFAULT NULL,
  `status_acc` enum('Disetujui','Ditinjau','Ditolak','Draft','AccDosenPA') DEFAULT NULL,
  `catatan_revisi` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `pengajuan_krs`
--

INSERT INTO `pengajuan_krs` (`id_pengajuan`, `id_krs`, `id_mahasiswa`, `id_dosen_acc`, `tanggal_pengajuan`, `status_acc`, `catatan_revisi`) VALUES
(21, 11, 1000, NULL, '2026-06-07', 'Disetujui', NULL),
(22, 11, 1005, NULL, '2026-06-21', 'Disetujui', NULL);

-- --------------------------------------------------------

--
-- Struktur dari tabel `prodi`
--

CREATE TABLE `prodi` (
  `id_prodi` int(10) NOT NULL,
  `nama_prodi` varchar(50) NOT NULL,
  `id_kaprodi` int(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `prodi`
--

INSERT INTO `prodi` (`id_prodi`, `nama_prodi`, `id_kaprodi`) VALUES
(111, 'Sistem dan Teknologi Informasi', 10004),
(122, 'Bisnis Digital', 0),
(133, 'Kewirausahaan', 0);

-- --------------------------------------------------------

--
-- Struktur dari tabel `ruang`
--

CREATE TABLE `ruang` (
  `id_ruang` int(11) NOT NULL,
  `nama_ruang` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `ruang`
--

INSERT INTO `ruang` (`id_ruang`, `nama_ruang`) VALUES
(3, '1.01'),
(4, '2.02'),
(5, '3.04');

--
-- Indexes for dumped tables
--

--
-- Indeks untuk tabel `admin`
--
ALTER TABLE `admin`
  ADD PRIMARY KEY (`id_admin`),
  ADD UNIQUE KEY `nama_admin` (`nama_admin`);

--
-- Indeks untuk tabel `dosen`
--
ALTER TABLE `dosen`
  ADD PRIMARY KEY (`id_dosen`),
  ADD UNIQUE KEY `nama_dosen` (`nama_dosen`);

--
-- Indeks untuk tabel `jadwal`
--
ALTER TABLE `jadwal`
  ADD PRIMARY KEY (`id_jadwal`);

--
-- Indeks untuk tabel `kelas`
--
ALTER TABLE `kelas`
  ADD PRIMARY KEY (`id_kelas`);

--
-- Indeks untuk tabel `krs`
--
ALTER TABLE `krs`
  ADD PRIMARY KEY (`id_krs`);

--
-- Indeks untuk tabel `krs_detail`
--
ALTER TABLE `krs_detail`
  ADD PRIMARY KEY (`id_detail`);

--
-- Indeks untuk tabel `mahasiswa`
--
ALTER TABLE `mahasiswa`
  ADD PRIMARY KEY (`id_mahasiswa`),
  ADD UNIQUE KEY `nama_mahasiswa` (`nama_mahasiswa`);

--
-- Indeks untuk tabel `matkul`
--
ALTER TABLE `matkul`
  ADD PRIMARY KEY (`id_matkul`);

--
-- Indeks untuk tabel `pengajuan_krs`
--
ALTER TABLE `pengajuan_krs`
  ADD PRIMARY KEY (`id_pengajuan`);

--
-- Indeks untuk tabel `prodi`
--
ALTER TABLE `prodi`
  ADD PRIMARY KEY (`id_prodi`);

--
-- Indeks untuk tabel `ruang`
--
ALTER TABLE `ruang`
  ADD PRIMARY KEY (`id_ruang`);

--
-- AUTO_INCREMENT untuk tabel yang dibuang
--

--
-- AUTO_INCREMENT untuk tabel `admin`
--
ALTER TABLE `admin`
  MODIFY `id_admin` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT untuk tabel `dosen`
--
ALTER TABLE `dosen`
  MODIFY `id_dosen` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=100221;

--
-- AUTO_INCREMENT untuk tabel `jadwal`
--
ALTER TABLE `jadwal`
  MODIFY `id_jadwal` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT untuk tabel `kelas`
--
ALTER TABLE `kelas`
  MODIFY `id_kelas` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT untuk tabel `krs`
--
ALTER TABLE `krs`
  MODIFY `id_krs` int(6) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT untuk tabel `krs_detail`
--
ALTER TABLE `krs_detail`
  MODIFY `id_detail` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=36;

--
-- AUTO_INCREMENT untuk tabel `mahasiswa`
--
ALTER TABLE `mahasiswa`
  MODIFY `id_mahasiswa` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1007;

--
-- AUTO_INCREMENT untuk tabel `matkul`
--
ALTER TABLE `matkul`
  MODIFY `id_matkul` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=28343;

--
-- AUTO_INCREMENT untuk tabel `pengajuan_krs`
--
ALTER TABLE `pengajuan_krs`
  MODIFY `id_pengajuan` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

--
-- AUTO_INCREMENT untuk tabel `prodi`
--
ALTER TABLE `prodi`
  MODIFY `id_prodi` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1232343;

--
-- AUTO_INCREMENT untuk tabel `ruang`
--
ALTER TABLE `ruang`
  MODIFY `id_ruang` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
