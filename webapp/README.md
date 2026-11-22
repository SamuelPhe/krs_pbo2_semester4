# KRS PHP web application

This folder provides a browser entry point with a login layout based on the original NetBeans Swing form. It authenticates against the original `krs_db.sql` tables and displays live row counts after login. It includes Edit Profile, Admin KRS session management, student KRS submission/course selection, and Dosen PA review. It is separate from GitHub Pages, which cannot execute PHP or connect to MySQL.

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

The browser version currently includes authentication, database-backed dashboard counts, Edit Profile, Admin management of KRS sessions, student KRS submission/course selection, and Dosen PA review. Profile changes use prepared statements, CSRF validation, output escaping, and the shared PBKDF2-SHA256 password format. Admin session management checks overlapping dates and blocks deletion when submissions are linked. Student submissions enforce advisor assignment, matching active sessions, duplicate-course and timetable-conflict checks, a 24-SKS maximum, and locking before review. Dosen PA review is restricted to assigned students and supports per-course approval or revision notes; successful review moves the application to `AccDosenPA`, while a revision returns it to the student. Kaprodi finalization and reports remain unported. The pages reuse Swing background assets and table concepts but are not pixel-identical to the desktop UI.

## Local server

Run `php -S 127.0.0.1:8081 -t webapp` from the repository root after configuring environment variables.
