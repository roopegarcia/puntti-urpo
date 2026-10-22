# Changelog

## Unreleased

- Added `./deploy/publish` for committed frontend updates, with read-only preflight, production source guards, separate WEB_RELEASE tagging, frontend-only recreation and verification-triggered rollback.

- Published the authorized isolated AMD64 production stack on the NUC: pinned multi-stage images, nginx/API routing, Secure sessions, private PostgreSQL, health/resource limits and rotated logs.
- Added private custom-format database backups, verified scratch restore and daily scheduling; public HTTPS, both accounts, private records and logout verified; operation details are tracked in DEPLOYMENT.md. Production starts with an empty owner account and fresh credentials.

- Added two manually provisioned session accounts (private owner and read-only demo), BCrypt configuration, CSRF-protected login/logout, generic security errors and a bounded login budget.
- Added V6 ownership backfill, account-scoped workout/nested/catalogue access and invented demo records; owner and demo cannot access each other's workouts or private custom catalogue data.
- Added Finnish login/logout, session-expiry recovery that preserves same-account drafts, demo controls and security/integration tests. Local account setup tools/documentation added; deployment remains separate.

- Translated the ten built-in exercises to Finnish with V5, preserving catalogue IDs, custom records and workout references; added PostgreSQL upgrade/data-preservation and conflict-rollback coverage.

- Added the reference-inspired author footer with GitHub, portfolio and blog links, responsive wrapping and theme-aware colors.

- Replaced app header/footer branding with Roope's supplied logo and matched the hero, favicon, empty states and exercise/workout icons to its mark.

### Milestone 5 — Finnish React frontend (2026-10-22)

- Added exact-pinned React/TypeScript/Vite project with lockfile, local proxy, build and test scripts.
- Added responsive exercise browsing/search/custom editing, workout navigation/creation/metadata/notes, logged exercises and editable/draft/completed sets.
- Added comma-decimal inputs, optional RPE, next-set copying with cleared effort, finish timing and confirmed deletion.
- Rendered server-derived approximate RIR and explicitly repetition-based e1RM with visible eligibility/effort warnings; no frontend strength calculation.
- Added light/dark themes, semantic tokens, original SVG identity, labelled controls, keyboard focus/skip link and responsive numeric entry.
- Added explicit-save reliability, unsaved/pending navigation guards, stale GET cancellation and partial-creation recovery; fixed browser-observed post-save navigation/focus races.
- Added 30 frontend tests and semantic accessibility audits, documented manual browser checks and limits, startup commands and source-based learning material. No backend API/schema change, authentication, progression analytics or deployment.

### Milestone 4 — RPE/RIR and repetition-based e1RM (2026-10-22)

- Added pure BigDecimal training calculations and immutable derived-result/status/warning types, separated from entities and persistence.
- Added completed-set approximate RIR and eligible-set Epley estimates with explicit unavailable reasons, effort warnings and a documented single-repetition convention.
- Added read-only `estimates` to all set responses, including nested workout/entry reads; existing records require no migration or backfill.
- Added all supported RPE-step, invalid-input, precision/rounding and eligibility tests; added HTTP/PostgreSQL coverage for consistent responses and refreshed results after updates.
- Extended Swagger descriptions, calculation policy/research, tutorial and technical documentation. RPE-aware estimation is documented separately and remains unimplemented.

### Milestone 3 — workout domain and requested catalogue (2026-10-22)

- Added V3 to use the ten exact requested English exercise names, preserving equivalent IDs and all custom exercise rows.
- Added V4 and JPA mappings for workouts, ordered logged exercises and sets, with required foreign keys, explicit delete cascades/restrictions and unique sibling positions.
- Added read/create/replace/delete APIs, nested workout detail, paginated summaries, exercise/set append and editable positions/notes.
- Added exact kg/RPE decimal storage, draft/completed-set validation, WARMUP/WORKING types and date/instant handling. RIR/e1RM calculations remain deferred.
- Added migration upgrade coverage, workout service unit tests and PostgreSQL HTTP tests for persistence, validation, ordering rollback, deletion isolation and catalogue-reference restrictions.
- Fixed omitted draft completion input and explicitly documented 201/204 success statuses in Swagger.
- Extended API/database/architecture/operation docs, decisions, Java tutorial and learning log. No frontend, authentication, deployment or formula implementation added.

### Milestone 2 — exercise catalogue (2026-10-22)

- Added an Exercise JPA entity, PostgreSQL schema/constraints and eight Finnish built-in seed exercises through Flyway V1/V2.
- Added constructor-injected repository/service/controller layers, immutable DTO records and explicit mapping.
- Added paginated browsing and literal case-insensitive name search, create/read/replace APIs for custom exercises, and protection for built-in records.
- Added Bean Validation, consistent ProblemDetail errors, transaction rollback for conflicts and database-enforced unique names.
- Added Springdoc 3.1.1 OpenAPI/Swagger UI and ECS structured console logs; kept API access local-only.
- Added five unit tests and eight catalogue HTTP/database integration tests; updated the three foundation integration tests for the new schema.
- Updated the tutorial, ER/request-flow diagrams, API/database/operation documentation and architecture decisions. No workout relationships, deletion API, authentication or frontend was added.

### Milestone 1 — foundation completed (2026-10-22)

- Added isolated PostgreSQL 18.6 Compose setup, a persistent named volume, loopback binding, health check and environment example.
- Added JDBC/HikariCP and Flyway initialization without premature domain tables or SQL migrations.
- Added Actuator health returning only aggregate UP/DOWN status, with database failure reflected as HTTP 503 after startup.
- Added three full-application integration tests against disposable real PostgreSQL, covering connectivity, Flyway initialization and health during database outage/recovery.
- Verified executable-JAR operation, database stop/start persistence, health recovery and hidden response details.
- Updated the tutorial, architecture, database, API, operation and decision documentation. Changed the learning workflow to complete milestones before tutorial review.

### Milestone 1 — initial checkpoint (2026-10-21)

- Added a Java 25 / Spring Boot 4.1.1 backend with Web MVC and embedded Tomcat.
- Added official Maven Wrapper 3.3.4 scripts, pinned to Maven 3.10.0 with a distribution checksum.
- Bound local development to loopback with an environment-configurable port and hidden HTTP error details.
- Added project documentation and a progressive Java tutorial based on the actual startup code.
- PostgreSQL, Flyway, health checks and automated tests were deferred at this initial checkpoint and completed on 2026-10-22 as recorded above.
