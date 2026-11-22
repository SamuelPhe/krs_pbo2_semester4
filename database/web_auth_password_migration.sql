-- Run after importing krs_db.sql and before the first login.
-- The login code upgrades legacy plaintext values to the shared PBKDF2-SHA256
-- format. These columns must be wide enough for the resulting hashes.
ALTER TABLE `mahasiswa` MODIFY COLUMN `password` VARCHAR(255) NOT NULL;
ALTER TABLE `dosen` MODIFY COLUMN `password` VARCHAR(255) NOT NULL;
ALTER TABLE `admin` MODIFY COLUMN `password` VARCHAR(255) NOT NULL;
