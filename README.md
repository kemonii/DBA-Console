# DBA-Console

A REST API for administering Oracle Database 21c Express Edition.

## Overview

DBA-Console exposes common database administration tasks for Oracle Database 21c XE through a consistent HTTP API. It is intended for database administrators who manage XE instances and want backup, performance, and security operations available from a single service. The API is independent of any client, and an Angular frontend in [`frontend/`](frontend) provides the user interface.

## Modules

| Module | Description | Status |
|---|---|---|
| Backup & Recovery | Data Pump exports (`expdp`) by table, schema, or full database; recovery with `impdp` and Flashback. | Planned |
| Query Tuning & Performance | Execution plans, optimizer statistics, and index maintenance; monitoring of the instance, sessions, and locks. | Available |
| Security | Users, roles, privileges, and profiles; auditing; tablespace and datafile management. | Available |
| File Management | Oracle directory objects and the dump and log files stored in them. | API available |

The endpoints available in this version are the health check described under [Getting started](#getting-started), the [File Management endpoints](#file-management-endpoints), the [Tablespace endpoints](#tablespace-endpoints), the [User, role, privilege and profile endpoints](#user-role-privilege-and-profile-endpoints) and the [Auditing endpoints](#auditing-endpoints). The frontend has an empty page for each module and one example screen.

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

## Tablespace endpoints

All paths start with `/api/security/tablespaces` and use the statements from the course (class 4). Tablespace names are validated as Oracle identifiers. The full request and response schemas are in Swagger UI.

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | List tablespaces with total, used and free space in MB (usage script from class 4: `DBA_DATA_FILES` and `DBA_FREE_SPACE`) |
| `POST` | `/` | `CREATE TABLESPACE ... DATAFILE ... SIZE ... AUTOEXTEND ON NEXT ... MAXSIZE ...`. Body: `{"name": "VENTAS", "fileName": "ventas01.dbf", "sizeMb": 2, "nextMb": 1, "maxSizeMb": 1024}`; omit `maxSizeMb` for `UNLIMITED` |
| `DELETE` | `/{name}?confirm=true` | `DROP TABLESPACE ... INCLUDING CONTENTS AND DATAFILES`; without `confirm=true` the request is rejected, and `SYSTEM` and `SYSAUX` are always refused |
| `GET` | `/{name}/datafiles` | List the datafiles of a tablespace (`DBA_DATA_FILES`) |
| `POST` | `/{name}/datafiles` | `ALTER TABLESPACE ... ADD DATAFILE ... SIZE ...`. Body: `{"fileName": "ventas02.dbf", "sizeMb": 256}` |
| `POST` | `/{name}/datafiles/{fileId}/resize` | `ALTER DATABASE DATAFILE ... RESIZE ...`. Body: `{"sizeMb": 5}` |

New datafiles are always created in `DATAFILE_DIR`: the request gives only a plain file name ending in `.dbf`, and names with path separators are rejected with HTTP 400. Sizes are whole megabytes between 1 and 32767. Free space is measured inside the allocated size, so autoextensible datafiles can still grow; temporary tablespaces are not listed.

## User, role, privilege and profile endpoints

All paths start with `/api/security` and use the statements from the course ("Seguridad"). Names are validated as Oracle identifiers. Users and roles maintained by Oracle (`ORACLE_MAINTAINED = 'Y'`, such as `SYS`, `SYSTEM` or `DBA`) are never locked, dropped, or stripped of privileges.

| Method | Path | Description |
|---|---|---|
| `GET` | `/users` | List the users created by administrators (`DBA_USERS`) |
| `POST` | `/users` | `CREATE USER ... IDENTIFIED BY "..." [DEFAULT TABLESPACE ...] [TEMPORARY TABLESPACE ...] [QUOTA ... ON ...]`. Body: `{"username": "PRUEBA", "password": "...", "defaultTablespace": "USERS", "temporaryTablespace": "TEMP", "quotaMb": 50}`; `"unlimitedQuota": true` for `QUOTA UNLIMITED` |
| `POST` | `/users/{name}/lock` | `ALTER USER ... ACCOUNT LOCK` |
| `POST` | `/users/{name}/unlock` | `ALTER USER ... ACCOUNT UNLOCK` |
| `DELETE` | `/users/{name}?confirm=true` | `DROP USER ...`; add `cascade=true` to also drop the user's objects |
| `GET` | `/roles` | List roles (`DBA_ROLES`), the ones created by administrators first |
| `POST` | `/roles` | `CREATE ROLE ...`. Body: `{"name": "VENTAS_ROL"}` |
| `DELETE` | `/roles/{name}` | `DROP ROLE ...` |
| `GET` | `/grants/system-privileges` | List the system privileges that can be granted (`SYSTEM_PRIVILEGE_MAP`) |
| `GET` | `/grants/{grantee}` | System privileges, roles and object privileges of a user or role (`DBA_SYS_PRIVS`, `DBA_ROLE_PRIVS`, `DBA_TAB_PRIVS`) |
| `POST` | `/grants/system` | `GRANT <privilege> TO <grantee> [WITH ADMIN OPTION]`. Body: `{"privilege": "CREATE SESSION", "grantee": "PRUEBA", "adminOption": false}` |
| `DELETE` | `/grants/system?privilege=...&grantee=...` | `REVOKE <privilege> FROM <grantee>` |
| `POST` | `/grants/role` | `GRANT <role> TO <grantee> [WITH ADMIN OPTION]`. Body: `{"role": "CONNECT", "grantee": "PRUEBA"}` |
| `DELETE` | `/grants/role?role=...&grantee=...` | `REVOKE <role> FROM <grantee>` |
| `POST` | `/grants/object` | `GRANT <privileges> ON <owner>.<object> TO <grantee> [WITH GRANT OPTION]`. Body: `{"privileges": ["SELECT", "INSERT"], "owner": "HR", "object": "EMPLOYEES", "grantee": "PRUEBA"}` |
| `DELETE` | `/grants/object?privileges=SELECT,INSERT&owner=...&object=...&grantee=...` | `REVOKE <privileges> ON <owner>.<object> FROM <grantee>` |
| `GET` | `/profiles` | Profiles and their kernel and password limits (`DBA_PROFILES`) |
| `POST` | `/profiles` | `CREATE PROFILE ... LIMIT ...`. Body: `{"name": "APP_USER", "limits": {"SESSIONS_PER_USER": "2", "IDLE_TIME": "30", "FAILED_LOGIN_ATTEMPTS": "3"}}` |
| `PUT` | `/profiles/{name}` | `ALTER PROFILE ... LIMIT ...`, for example `{"limits": {"IDLE_TIME": "20"}}` on `DEFAULT` |
| `DELETE` | `/profiles/{name}` | `DROP PROFILE ... CASCADE` (its users get `DEFAULT`); `DEFAULT` itself is refused |
| `PUT` | `/users/{name}/profile` | `ALTER USER ... PROFILE ...`. Body: `{"profile": "APP_USER"}` |

Profile limits are `SESSIONS_PER_USER`, `CPU_PER_SESSION`, `CPU_PER_CALL`, `CONNECT_TIME`, `IDLE_TIME`, `LOGICAL_READS_PER_SESSION`, `LOGICAL_READS_PER_CALL`, `FAILED_LOGIN_ATTEMPTS`, `PASSWORD_LIFE_TIME`, `PASSWORD_REUSE_TIME`, `PASSWORD_REUSE_MAX`, `PASSWORD_LOCK_TIME` and `PASSWORD_GRACE_TIME`, each a whole number, `UNLIMITED` or `DEFAULT`. The grantee can be a user, a role or `PUBLIC`. Passwords cannot be bind parameters, so they are rejected when they contain double quotes or control characters, and written in double quotes. System privilege names are checked against `SYSTEM_PRIVILEGE_MAP`, and object privileges against the list from the course (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `ALTER`, `INDEX`, `REFERENCES`, `EXECUTE`, `DEBUG`, `FLASHBACK`, `READ`, `WRITE`, `ALL`).

## Auditing endpoints

All paths start with `/api/security/auditing` and use the statements and views from the course ("Auditoría Oracle"). This is traditional auditing: records are written to `SYS.AUD$` only while the `audit_trail` parameter is `DB` (or `DB, EXTENDED`). The application only reads the parameter, since changing it needs `ALTER SYSTEM SET audit_trail = DB SCOPE = SPFILE` and a database restart.

| Method | Path | Description |
|---|---|---|
| `GET` | `/parameter` | Value of `audit_trail` (`V$PARAMETER`) and whether it writes to the database trail |
| `GET` | `/options` | Statement options (`DBA_STMT_AUDIT_OPTS`) and audited tables (`DBA_OBJ_AUDIT_OPTS`, without Oracle's own schemas) |
| `POST` | `/session` | `AUDIT SESSION [WHENEVER [NOT] SUCCESSFUL]`. Body: `{"whenever": "ALWAYS"}`, `"SUCCESSFUL"` or `"NOT_SUCCESSFUL"` |
| `DELETE` | `/session?whenever=ALWAYS` | `NOAUDIT SESSION [WHENEVER ...]` |
| `POST` | `/tables` | `AUDIT <actions> ON <owner>.<table> BY ACCESS \| BY SESSION`. Body: `{"owner": "HR", "table": "JOBS", "actions": ["INSERT", "UPDATE", "DELETE"], "byAccess": true}` |
| `DELETE` | `/tables?owner=...&table=...&actions=INSERT,UPDATE` | `NOAUDIT <actions> ON <owner>.<table>` |
| `GET` | `/trail` | Newest rows of `DBA_AUDIT_TRAIL` |
| `GET` | `/trail/sessions` | Newest login attempts from `DBA_AUDIT_SESSION`, with the outcome decoded as in class (`Connected`, `Failed`, ...) |
| `GET` | `/trail/objects` | Newest audited actions on objects from `DBA_AUDIT_OBJECT` |

The three `/trail` endpoints accept an optional `username` and `limit` (default 100, at most 1000), both sent as bind values. Table actions are `INSERT`, `UPDATE`, `DELETE` and `SELECT`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branching, pull request, and code conventions.

## About

This project was developed as the final project for the EIF-402 Database Administration course at Universidad Nacional, Costa Rica.

## Query tuning and performance endpoints

The frontend at `/tuning` has execution plan, statistics/index maintenance, and monitoring screens.
All responses use `OperationResult`; Oracle errors retain their ORA code. Object names are validated
with `OracleIdentifiers.requireValid`. Index columns are plain column names (no SQL expressions).

| Method | Path | Description |
|---|---|---|
| POST | `/api/tuning/queries/plan` | Explain one statement; body `{"statement":"SELECT * FROM HR.JOBS"}` |
| GET | `/api/tuning/queries/statistics/{schema}` | Table NUM_ROWS and LAST_ANALYZED |
| POST | `/api/tuning/queries/statistics/{schema}/gather` | Gather schema statistics |
| POST | `/api/tuning/queries/statistics/{schema}/{table}/gather` | Gather table statistics |
| GET | `/api/tuning/queries/indexes/{schema}` | List indexes |
| POST | `/api/tuning/queries/indexes` | Create index; body `{"schema":"HR","table":"JOBS","name":"JOBS_IDX","columns":["JOB_ID"],"type":"BTREE"}`; type may also be `BITMAP` |
| POST | `/api/tuning/queries/indexes/{schema}/{index}/rebuild` | Rebuild index |
| POST | `/api/tuning/queries/indexes/{schema}/{index}/validate` | Validate structure and read INDEX_STATS |
| DELETE | `/api/tuning/queries/indexes/{schema}/{index}?confirm=true` | Drop index |
| GET | `/api/tuning/monitoring/instance` | Instance status |
| GET | `/api/tuning/monitoring/parameters` | Main system parameters |
| GET | `/api/tuning/monitoring/sessions` | Current sessions, open cursors and blocking session |
| GET | `/api/tuning/monitoring/sessions/programs` | Session counts by program |
| GET | `/api/tuning/monitoring/sessions/users` | Session counts by user |
| GET | `/api/tuning/monitoring/locks` | Blocking and waiting sessions from V$LOCK and V$SESSION |
| GET | `/api/tuning/monitoring/locks/objects` | Objects locked by sessions |
| DELETE | `/api/tuning/monitoring/sessions/{sid}/{serial}?confirm=true` | Kill a session only if the SID and serial still identify a blocker |

EXPLAIN PLAN and its hierarchical CONNECT BY read run inside one `ConnectionCallback`, using a
unique statement ID and deleting only that plan's rows afterward. ANALYZE INDEX and INDEX_STATS
also share one connection. Estimates are not actual execution timings. Open cursors use the
`opened cursors current` session statistic, avoiding counts of cached cursor entries.

The configured administrator needs access to DBA_TABLES, DBA_INDEXES, DBA_OBJECTS, the V$ views,
PLAN_TABLE and INDEX_STATS; execute permission on DBMS_STATS; and appropriate CREATE/ALTER/DROP
INDEX, ANALYZE ANY and ALTER SYSTEM privileges for the objects it administers. Oracle edition,
object type and ownership restrictions are reported by the database. Validation uses offline
VALIDATE STRUCTURE and can hold locks while it runs. Killing a session rolls back its uncommitted
work; both destructive operations have an explicit confirmation step in the screen.

### Manual verification on Oracle 21c XE

Use a disposable application schema and table, with the backend configured for XEPDB1:

1. Explain a SELECT on the test table and verify hierarchy and full-scan highlighting. Explain an
   invalid SELECT and verify the Oracle error appears; repeat to check pooled connections.
2. Gather table and schema statistics, then reload NUM_ROWS and LAST_ANALYZED.
3. Create a B-tree index and a bitmap index on suitable test columns. Reload, validate each,
   rebuild, and confirm dropping each test index. Check invalid identifiers are rejected.
4. Compare instance, parameters, sessions and session counts with SQL Developer. Open cursors
   should be nonnegative and visible for each session.
5. In two separate SQL Developer connections, update the same test row without committing to
   create a blocker/waiter pair. Refresh blocking locks and locked objects, verify the SID/serial,
   then confirm termination of the disposable blocking session. Refresh to verify the wait ends.
   Roll back remaining test work and remove the test objects.

Automated tests mock JDBC/HTTP and do not replace these Oracle integration checks.

Verification on the local Oracle 21c XE instance (2026-10-28): all seven monitoring
views responded successfully; execution plans returned the hierarchy and full-scan flag;
schema/table statistics reported 1000 test rows and LAST_ANALYZED; B-tree and bitmap
indexes passed create/rebuild/validate/drop; a two-connection blocking test was detected
and resolved by terminating the exact test blocker. The temporary schema was removed.
The three frontend screens were checked against the running backend. Automated suites:
314 backend tests (2 skipped, no failures) and 161 frontend tests passed; production
frontend build and Maven verify succeeded.
