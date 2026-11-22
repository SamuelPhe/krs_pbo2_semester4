# KRS Nostalgia — GitHub Codespaces

This configuration runs the original Java Swing KRS application in a private virtual desktop, with the uploaded `krs_db.sql` restored into a local MariaDB service.

## Start it

1. Open this repository on GitHub.
2. Select **Code → Codespaces → Create codespace on master**.
3. Wait for the Codespace setup to finish. The original KRS login form is started automatically.
4. Open the **Ports** tab and open **KRS desktop (private)**, port `6080`.

GitHub Codespaces forwards ports as **Private by default**. After the Codespace starts, confirm port `6080` shows **Private** in the Ports tab; do not change it to Public. GitHub authentication controls access; the port is not a public demo.

## Database

The Codespace starts its own MariaDB container and imports `krs_db.sql`, then applies `database/web_auth_password_migration.sql` so account passwords can be upgraded to the shared PBKDF2 format.

The database port is not forwarded. Its credentials are only for the local Codespaces compose network; do not reuse them elsewhere.

## What opens

The original `PBO_Project_2.login_form` Java Swing form opens inside the virtual desktop. The login form includes the original role selector and background art. After successful login, it opens the original `dashboard` form.

The Java source still has known differences between some CRUD queries and the uploaded SQL schema. Login and the dashboard are the initial nostalgia target; deeper KRS modules may need additional schema reconciliation before every action works.

## Stop and resume

Stop the Codespace when you are finished to avoid using its running time. When you resume it, the desktop and login app are started again automatically. The MariaDB data is kept in the Codespace's named volume while that Codespace is retained.

## Notes

- This does not deploy anything to GitHub Pages or a public web host.
- The SQL dump is in a public repository and contains legacy sample credentials and records. Treat those credentials as exposed; this private Codespace is for personal nostalgia/testing, not real accounts or sensitive data.
