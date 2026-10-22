# Changelog

## Unreleased

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
