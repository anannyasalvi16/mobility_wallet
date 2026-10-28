# Mobility Wallet Project Rules

## 1. Attribution Integrity
- Preserve the user's author attribution (`Made by Anannya Salvi • 2026`) across all Thymeleaf templates and UI footers.
- Do NOT sanitize, alter, or remove author attribution during repository security clean-up sweeps unless explicitly instructed by the user.

## 2. Secret & Credential Isolation
- Never commit real passwords, tokens, or personal LAN IPs to tracked files.
- Use `src/main/resources/application-local.properties` for machine-specific overrides (e.g., local database passwords). This file MUST be listed in `.gitignore`.
- Ensure `application-dev.properties` imports `optional:classpath:application-local.properties,optional:file:application-local.properties`.
- Ensure `.env*` is in `.gitignore`, with an explicit exception `!.env.example` containing only dummy placeholders.

## 3. Flyway & Database Invariants
- Never modify an existing Flyway migration script (e.g. `V1__init_schema.sql`) after it has been created and verified. Any future schema changes must be added as new versioned files (`V2__*.sql`).
- Set `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=1` so existing databases do not fail on migration.
- For production and development runtime, enforce `spring.jpa.hibernate.ddl-auto=validate`.
- For hermetic H2 test suites, disable Flyway (`spring.flyway.enabled=false`) and set `spring.jpa.hibernate.ddl-auto=create-drop` to avoid database engine dialect collisions.

## 4. Git Two-Phase Approval Protocol
- Always work on a task branch (`feature/<task-name>`), never directly on `main`.
- Never execute `git commit` or `git push` autonomously.
- After all verifications (`mvn clean verify`) pass, display:
  1. The full list of changed and untracked files
  2. `git status`
  3. `git diff --stat`
- STOP and request explicit user confirmation before committing.
- Request a second explicit confirmation before pushing to the remote repository.
