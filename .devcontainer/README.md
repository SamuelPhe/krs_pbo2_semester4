# KRS Nostalgia — GitHub Codespaces

This configuration runs the original Java Swing KRS application and a separate PHP web login/dashboard in one private Codespace. Both connect to the same MariaDB database restored from `krs_db.sql`.

## Open the browser-based web version

1. Open this repository on GitHub.
2. Select **Code → Codespaces → Create codespace on master** (or resume the existing KRS Codespace).
3. Wait for the Codespace to finish building. The MariaDB and PHP/Apache web containers start automatically.
4. Open the **Ports** tab in the Codespaces editor.
5. Find **KRS web (private)**, port `8081`, and choose **Open in Browser**.

The browser should display the KRS login page. After login, the current web version displays database-backed counts on a dashboard.

## Open the original Java Swing application

1. In the same Codespace, open the **Ports** tab.
2. Find **KRS desktop (private)**, port `6080`.
3. Choose **Open in Browser** to open the virtual desktop with the original `PBO_Project_2.login_form`.

The original desktop login form starts automatically after Codespaces setup.

## Port privacy

Keep ports `6080` and `8081` set to **Private**. Do not change them to Public. The repository's SQL dump includes legacy sample credentials and academic records; treat all values in it as exposed and use this setup only for personal nostalgia/testing. Do not use the database for real student records or production work.

If this Codespace was created before the PHP web service was added and port `8081` is missing, run **Codespaces: Rebuild Container** from the VS Code Command Palette, then reopen the Ports tab.

## Database

The Codespace starts a MariaDB container and imports `krs_db.sql`, then applies `database/web_auth_password_migration.sql` so the three account password columns can store PBKDF2 hashes.

The PHP web app and Java desktop app both use database `krs_db`. The database port is not forwarded to the browser; it is available only on the internal Compose network.

## Current web scope

The PHP web version currently supports role-based login and a dashboard with database row counts. It is not yet a complete web port of the original Swing CRUD, KRS submission, academic-advisor approval, finalization, or reporting modules. The GitHub Pages demo is a separate static simulation and does not connect to MariaDB.

## Stop and resume

Stop the Codespace when finished to avoid using its running time. When resumed, Compose services start again. MariaDB data is held in the named volume for as long as that Codespace and volume are retained.

## Notes

- This does not deploy anything to GitHub Pages or a public web host.
- Do not reuse the SQL dump's legacy credentials. Reset credentials before any use beyond local testing.
