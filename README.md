# DBA-Console

A REST API for administering Oracle Database 21c Express Edition.

## Overview

DBA-Console exposes common database administration tasks for Oracle Database 21c XE through a consistent HTTP API. It is intended for database administrators who manage XE instances and want backup, performance, and security operations available from a single service. The API is independent of any client, and an Angular frontend in [`frontend/`](frontend) provides the user interface.

## Modules

| Module | Description | Status |
|---|---|---|
| Backup & Recovery | Data Pump exports (`expdp`) by table, schema, or full database; recovery with `impdp` and Flashback. | Planned |
| Query Tuning & Performance | Execution plans, optimizer statistics, and index maintenance; monitoring of the instance, sessions, and locks. | Planned |
| Security | Users, roles, privileges, and profiles; auditing; tablespace and datafile management. | Planned |
| File Management | Oracle directory objects and the dump and log files stored in them. | API available |

The endpoints available in this version are the health check described under [Getting started](#getting-started) and the [File Management endpoints](#file-management-endpoints). The frontend has an empty page for each module and one example screen.

## Tech stack

- Java 17
- Spring Boot 3.5 (Web, JDBC, Validation)
- Spring `JdbcTemplate` for data access (no JPA or Hibernate: the application works with Oracle data dictionary views and DDL, where entity mapping adds nothing)
- Oracle JDBC driver (`ojdbc11`)
- Oracle Data Pump command-line utilities (`expdp`, `impdp`) for logical backups
- springdoc-openapi for OpenAPI documentation and Swagger UI
- JUnit 5 for testing
- Maven
- Angular 22 (standalone components, signals, Signal Forms) with Vitest for the frontend

## Prerequisites

- JDK 17 or later
- Oracle Database 21c XE
- Node.js 22.22.3+, 24.15+, or 26+ (odd-numbered versions 23 and 25 are not supported) and npm (frontend only)

Maven does not need to be installed: the Maven Wrapper (`mvnw` / `mvnw.cmd`) downloads the version the project uses on first run.

The application must run on the same host as the database. Backups invoke the `expdp` and `impdp` utilities, which must be available on the `PATH`, and the dump and log files they produce are written to the database server's file system.

## Configuration

The application reads the following settings. None of them have default values, and credentials must never be written to a committed file.

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | JDBC URL of the pluggable database service | `jdbc:oracle:thin:@//localhost:1521/XEPDB1` |
| `DB_USER` | Database user with administrative privileges | `your_admin_user` |
| `DB_PASS` | Password for `DB_USER` | `your_password` |
| `DATAFILE_DIR` | Directory where new datafiles are created | `/opt/oracle/oradata/XE/XEPDB1` |

`DB_URL` must point to the `XEPDB1` service (the pluggable database), not to the XE root container. Regular users without the `C##` prefix can only be created inside a pluggable database; connecting to the root container restricts you to common users.

`DB_USER` needs administrative privileges, since the application reads `DBA_*` and `V$` views, runs DDL, and performs full database exports. On a local installation, connecting as `SYSTEM` to `XEPDB1` is sufficient.

Datafile locations differ between Windows and Linux installations, which is why `DATAFILE_DIR` is configurable.

### `.env` file (recommended)

Copy [`.env.example`](.env.example) to `.env` in the project root and fill in your values. The application reads it when started from the project root, and `.env` is ignored by Git.

```powershell
Copy-Item .env.example .env
```

`.env` is read as a Java properties file, so write Windows paths with forward slashes, for example `DATAFILE_DIR=C:/app/oracle/oradata/XE/XEPDB1`. A backslash starts an escape sequence.

### Environment variables

Environment variables take precedence over `.env`, so they can also be set in the shell, for example on a machine without a `.env` file.

PowerShell:

```powershell
$env:DB_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:DB_USER = "your_admin_user"
$env:DB_PASS = "your_password"
$env:DATAFILE_DIR = "C:\app\oracle\oradata\XE\XEPDB1"
```

CMD:

```bat
set DB_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
set DB_USER=your_admin_user
set DB_PASS=your_password
set DATAFILE_DIR=C:\app\oracle\oradata\XE\XEPDB1
```

bash:

```bash
export DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
export DB_USER="your_admin_user"
export DB_PASS="your_password"
export DATAFILE_DIR="/opt/oracle/oradata/XE/XEPDB1"
```

## Getting started

1. Clone the repository:

   ```bash
   git clone https://github.com/kemonii/DBA-Console.git
   cd DBA-Console
   ```

2. Create the `.env` file or set the environment variables described in [Configuration](#configuration).

3. Start the application from the project root:

   ```powershell
   .\mvnw.cmd spring-boot:run   # Windows
   ```

   ```bash
   ./mvnw spring-boot:run       # Linux and macOS
   ```

4. Open the API documentation at <http://localhost:8080/swagger-ui.html>.

5. Verify the database connection:

   ```bash
   curl http://localhost:8080/api/health
   ```

6. Start the frontend in a second terminal (the backend must be running):

   ```bash
   cd frontend
   npm install
   npm start
   ```

   Open <http://localhost:4200>. The development server proxies every `/api` request to the backend on port 8080 (see `frontend/proxy.conf.json`), so no CORS configuration is needed. The header shows whether the backend can reach the database.

To build and run the backend tests (no database required):

```bash
./mvnw verify   # .\mvnw.cmd verify on Windows
```

To build and run the frontend tests:

```bash
cd frontend
npm test -- --no-watch
npm run build
```

## Project structure

```
src/main/java/una/dbadmin
├── DbAdminApplication.java
├── common/                    Shared infrastructure
│   ├── OperationResult.java
│   ├── GlobalExceptionHandler.java
│   ├── HealthController.java
│   └── OracleIdentifiers.java
├── backup/                    Backup & Recovery
├── tuning/
│   ├── queries/               Execution plans, statistics, indexes
│   └── monitoring/            Instance, sessions, locks
├── security/
│   ├── tablespaces/           Tablespace management
│   ├── auditing/              Auditing
│   └── users/                 User management
└── files/                     Backup file management

frontend/src/app
├── core/                      API client, OperationResult model, health monitor
├── layout/                    Application shell, navigation menu, connection status
├── backup/                    Backup & Recovery page and routes
├── tuning/                    Query Tuning & Performance page and routes
├── security/                  Security page and routes
├── files/                     File Management page and routes
└── example/                   Reference screen (form, API call, result) to copy
```

The frontend follows the same idea. Each module folder under `frontend/src/app` holds its own page and a `<module>.routes.ts` file that the application loads lazily, so each person edits only their own folder. To build a screen, copy the pattern in `example/` and call the API through `ApiClient` in `core/`, which returns the `OperationResult` fields described below.

The backend code is organized by module rather than by layer. Each feature lives in its own package and contains its own controller and service, so contributors working on different modules rarely touch the same files. Code shared by all modules belongs in `common/`.

## API conventions

Every endpoint returns an `OperationResult` with the following fields:

| Field | Type | Description |
|---|---|---|
| `ok` | boolean | `true` if the operation succeeded |
| `message` | string | Human-readable description of the outcome |
| `data` | any | Response payload, or `null` when there is none |

Successful response:

```json
{
  "ok": true,
  "message": "Database connection is available",
  "data": null
}
```

Errors use the same format with `ok` set to `false`. Invalid input returns HTTP 400. Database errors return HTTP 500 and keep the Oracle error code when one is available:

```json
{
  "ok": false,
  "message": "ORA-00942: table or view does not exist",
  "data": null
}
```

## File Management endpoints

All paths start with `/api/files`. Directory and user names are validated as Oracle identifiers. The full request and response schemas are in Swagger UI.

| Method | Path | Description |
|---|---|---|
| `GET` | `/directories` | List directory objects (`DBA_DIRECTORIES`) |
| `POST` | `/directories` | Create or replace a directory object. Body: `{"name": "BACKUPS", "path": "/u01/backups"}` |
| `DELETE` | `/directories/{name}` | Drop a directory object |
| `POST` | `/directories/{name}/grants` | Grant `READ, WRITE` on the directory to a user. Body: `{"user": "SCOTT"}` |
| `DELETE` | `/directories/{name}/grants/{user}` | Revoke `READ, WRITE` on the directory from a user |
| `GET` | `/directories/{name}/files` | List the `.dmp` and `.log` files in the directory, with size and last modified date |
| `GET` | `/directories/{name}/files/{file}/content` | View a `.log` file (limited to 1000 lines and 256 KB; `truncated` tells whether there is more) |
| `DELETE` | `/directories/{name}/files/{file}?confirm=true` | Delete a backup file; without `confirm=true` the request is rejected |

File names must be plain names ending in `.dmp` or `.log`, made of letters, digits, `_`, `.` and `-`. Names with path separators, `..`, or other characters are rejected with HTTP 400, and the resolved path (including symbolic links) must stay inside the directory's path. Files are read from the directory's path on the host where the application runs.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branching, pull request, and code conventions.

## About

This project was developed as the final project for the EIF-402 Database Administration course at Universidad Nacional, Costa Rica.
