# Contributing to DBA-Console

## Branching

- Create one branch per person or per issue. Do not work directly on `main`.
- Name branches by type and scope:
  - `feature/<scope>` for new functionality, for example `feature/security-tablespaces`
  - `fix/<scope>` for bug fixes, for example `fix/health-timeout`
  - `docs/<scope>` for documentation changes, for example `docs/readme-configuration`

## Pull requests

- Open a pull request to `main`. Direct pushes to `main` are not allowed.
- Reference the related issue in the description with `Closes #n`.
- Keep pull requests small and focused on a single change.
- Make sure `mvn verify` passes before requesting a review.

## Commit messages

- Write a short subject in the imperative mood, for example `Add tablespace listing endpoint`.
- Keep the subject under about 72 characters and leave out the trailing period.
- Add a body after a blank line when the change needs explanation.

## Code conventions

- **Use techniques covered in the course.** Implement each feature with the commands and views taught in the EIF-402 course material (for example, `expdp`/`impdp` for backups and `EXPLAIN PLAN` for execution plans). Discuss with the team before introducing a different Oracle feature.
- **Package by module.** Each module lives in its own package under `una.dbadmin` (`backup`, `tuning.queries`, `tuning.monitoring`, `security.tablespaces`, `security.auditing`, `security.users`, `files`). Shared code goes in `common`.
- **Directory objects belong to `files`.** The `files` module creates Oracle directory objects and grants access to them; `backup` only references them by name.
- **Controllers handle HTTP only.** A controller maps requests, validates input, and delegates to its service. It must not contain SQL.
- **Services hold all SQL and logic.** All database access goes through `JdbcTemplate` in the module's service.
- **Responses use `OperationResult`.** Throw `IllegalArgumentException` for invalid input and let `GlobalExceptionHandler` produce the error response.
- **Validate DDL identifiers.** Identifiers such as user, tablespace, or directory names cannot be bind parameters in DDL statements or `expdp`/`impdp` commands, so pass them through `OracleIdentifiers.requireValid` before concatenating them. Values in regular queries must always use bind parameters.
- **Never commit credentials.** Configuration comes from environment variables. When you add a new variable, add it to `.env.example` with a placeholder value and document it in the README.

## Local setup

Follow the [Configuration](README.md#configuration) and [Getting started](README.md#getting-started) sections of the README.
