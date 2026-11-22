# KRS Academic Management System

The KRS Academic Management System is a Java desktop application developed as an **Object-Oriented Programming (PBO), Semester 4** course project. It supports a course-registration workflow involving students, academic advisors, and administrators.

> **Project type:** Desktop application  
> **Project context:** Academic project — Semester 4  

## 1. Application preview

The repository includes UI design and application preview images.

### Home page
![KRS home page](src/main/resources/desain/home%20page%20%282%29.png)

### Login page
![KRS login page](src/main/resources/desain/Login%20%282%29.png)

### KRS detail
![KRS detail](src/main/resources/desain/Detail%20KRS.png)

### Schedule management
![Schedule management](src/main/resources/desain/Jadwal.png)

## 2. Main features

- Role-based login for **students, lecturers, and administrators**
- Management of study programs, students, lecturers, courses, classes, rooms, and schedules
- Student course-registration submission and course selection
- Duplicate-course and timetable-conflict checks
- Academic-advisor (Dosen PA) review and approval
- Study-program coordinator (Kaprodi) finalization
- KRS archiving and report printing with JasperReports

## 3. Technology stack

- Java (Maven compiler release 8; JDK 17 is recommended for the current build environment)
- Java Swing
- MySQL and JDBC
- MySQL Connector/J 8.3.0
- JasperReports 7.0.3
- NetBeans AbsoluteLayout
- NetBeans GUI Builder

## 4. Architecture overview

The application is structured as a Java Swing desktop program. UI forms support role-specific workflows and call the database access class for authentication and data operations. The database layer uses JDBC and prepared statements for parameterized queries. JasperReports is used for report generation.

```mermaid
flowchart TD
    U[Student, Lecturer, or Admin] --> UI[Java Swing Forms]
    UI --> FLOW[Course Registration and Academic Workflow]
    FLOW --> AUTH[Role Authentication and Session]
    FLOW --> DAL[Database Access Class]
    DAL --> JDBC[JDBC / MySQL Connector]
    JDBC --> DB[(MySQL Database)]
    FLOW --> REPORT[JasperReports]
    REPORT --> PDF[Printable KRS Reports]
```

## 5. Demo and local setup

A **browser-based simulation** of the academic workflows is available in [`demo/index.html`](demo/index.html). It demonstrates student course selection, duplicate and timetable-conflict checks, Dosen PA approval, and study-program finalization using fictional sample data. It is a web adaptation for portfolio viewing, not the original Java Swing executable, and it does not connect to the campus database.

To publish the demo, enable GitHub Pages at **Settings → Pages → Build and deployment → Source: GitHub Actions**, then run **Deploy browser demo** from the Actions tab. After deployment, the public URL will be: https://samuelphe.github.io/krs_pbo2_semester4/.

### Run the original desktop app and the web login in Codespaces

The repository can run both interfaces in the same private GitHub Codespace. They use the same local MariaDB database named `krs_db`.

**Open the browser-based web version:**

1. Open this repository on GitHub and select **Code → Codespaces**.
2. Select **Create codespace on master** (or resume the existing KRS Codespace).
3. Wait for the Codespace setup to finish. The MariaDB and PHP/Apache web services start automatically.
4. Open the **Ports** tab at the bottom of the Codespaces editor.
5. Find **KRS web (private)** on port `8081`, then select **Open in Browser**.

The web page should show the KRS login screen. After a successful login it shows a dashboard with counts read from the Codespaces database.

**Open the original Java Swing desktop version:**

1. In the same Codespace, open the **Ports** tab.
2. Find **KRS desktop (private)** on port `6080`.
3. Select **Open in Browser** to open the virtual desktop containing the original Java Swing login window.

Both ports must remain **Private**. Do not change either port to Public. If a Codespace was created before the PHP web service was added and port `8081` does not appear, run **Codespaces: Rebuild Container** from the VS Code Command Palette, then check the Ports tab again.

The PHP web version is currently a browser-based login and database-summary dashboard. It is **not yet a complete web port** of the Java Swing CRUD, KRS submission, approval, and reporting modules. The separate GitHub Pages demo remains a static browser simulation and does not connect to this database.

The original Java application can still be run locally:

1. Install JDK 17 and Maven.
2. Prepare a MySQL or MariaDB database named `krs_db`. The repository contains `krs_db.sql`, a historical coursework snapshot used by the Codespaces setup.
3. For local experiments, restore a trusted copy of the coursework database. Some legacy Java Swing module queries still differ from this snapshot, so do not treat the dump as a verified production schema.
4. Set the database environment variables:
   - `DB_URL` (optional; default: `jdbc:mysql://127.0.0.1:3306/krs_db?serverTimezone=UTC`)
   - `DB_USER`
   - `DB_PASSWORD`
5. Build the application:

   ```bash
   mvn clean compile
   ```

6. Run the application:

   ```bash
   mvn exec:java
   ```

The provided migration script `database/password_hash_migration.sql` should only be applied to a backed-up database copy after reviewing its effect. No demo account or real student data is provided.

## 6. Project structure

- `src/main/java/PBO_Project_2/` — Swing forms, academic workflows, session, password, and database classes
- `src/main/resources/desain/` — UI design and preview images
- `pom.xml` — Maven compiler and dependency configuration
- `database/` — database migration note

## 7. Copyright and license

This is a collaborative academic project developed by the following team members:

- Davent Gravando
- Marsha Marthalina
- Samuel
- Wendy Lo

This attribution recognizes the project team and does not claim that the entire project or every contribution is solely owned by Samuel Phe. Ownership and rights in jointly created materials and individual contributions remain with the respective authors or rightsholders, subject to any applicable team, course, institutional, or other agreements.

No open-source license is granted by this repository for project materials created by the team. Permission to copy, modify, redistribute, sublicense, or reuse team-created materials should be obtained from the relevant rightsholders. Java, MySQL Connector/J, JasperReports, NetBeans components, and other third-party libraries remain subject to their own licenses and notices. See the [LICENSE](LICENSE) file.
---

This is an academic portfolio project, not a production-ready university information system. Validate the database schema and test the application using a disposable database copy before any demonstration.


## Project team and credits

This academic project was developed collaboratively by the following team members:

- Davent Gravando
- Marsha Marthalina
- Samuel
- Wendy Lo

The names above acknowledge the project team as a whole. Individual module-level responsibilities are not assigned here because a verified task-allocation record was not provided. This credit does not imply that every member authored every file or that each contribution was identical.


## Copyright and licensing

This is a collaborative academic project credited to the team members listed above. This notice does not assign or transfer copyright and does not make a legal determination about ownership of jointly created work. Rights in individual contributions remain with their respective authors or rightsholders, subject to any applicable team, course, institutional, or other agreements. No open-source license is granted by this repository. Permission to reuse team-created materials should be obtained from the relevant rightsholders. Third-party libraries and assets remain subject to their own licenses and notices. See [LICENSE](LICENSE).
