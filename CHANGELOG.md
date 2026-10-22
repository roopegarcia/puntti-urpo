# Changelog

## Unreleased

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
