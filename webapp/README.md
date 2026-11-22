# KRS PHP web login (database-backed)

This folder adds a browser entry point with a login layout based on the original NetBeans Swing form. It authenticates against the uploaded `krs_db.sql` tables and displays live row counts after login. It is separate from GitHub Pages, which cannot execute PHP or connect to MySQL.

## Requirements

- PHP 8.1 or newer
- PDO MySQL extension
- MySQL or MariaDB
- HTTPS in production

## Database setup

1. Import `krs_db.sql` into a database named `krs_db` (or configure another name).
2. Run `database/web_auth_password_migration.sql` once.
3. The migration expands the three account password columns to `VARCHAR(255)`. On successful login, legacy plaintext values are upgraded to PBKDF2-SHA256. The same format will be supported by the Java desktop application's login code.
4. The SQL dump is in a public repository and contains legacy plaintext passwords and personal/sample records. Treat those passwords as exposed and reset them before any public deployment.

## Hosting environment

Set the following environment variables in the PHP hosting panel. Do not commit real database credentials:

- `DB_HOST` (default `127.0.0.1`)
- `DB_PORT` (default `3306`)
- `DB_DATABASE` (default `krs_db`)
- `DB_USER` (required)
- `DB_PASSWORD` (required)

Set the web document root to this `webapp` directory or copy its contents to the PHP hosting document root. GitHub Pages is not a PHP hosting service.

## Login roles

The original role choices are supported: Mahasiswa, Admin, Kaprodi, Dosen PA, and Dosen. Dosen roles are checked against the `posisi` value in the `dosen` table. Password matching uses the original account names from the dump and prepared statements.

The first web version includes authentication and database-backed dashboard counts. The remaining Swing CRUD screens still need to be ported and reconciled with the SQL schema; the existing Java source currently references some columns that are not present in the uploaded dump.

## Local server

Run `php -S 127.0.0.1:8081 -t webapp` from the repository root after configuring environment variables.
