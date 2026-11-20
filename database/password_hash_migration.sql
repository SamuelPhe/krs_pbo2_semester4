-- Back up the local krs_db database before applying this migration.
ALTER TABLE mahasiswa MODIFY COLUMN password VARCHAR(255) NOT NULL;
ALTER TABLE dosen MODIFY COLUMN password VARCHAR(255) NOT NULL;
ALTER TABLE admin MODIFY COLUMN password VARCHAR(255) NOT NULL;
