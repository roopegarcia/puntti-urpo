# Architecture decisions

## 2026-10-21 — Java 25, Spring Boot 4.1.1 and Maven Wrapper

**Context:** Roope is learning Java through a portfolio-quality application.

**Options:** Java 21 or 25; Maven or Gradle; global Maven or a repository wrapper.

**Choice:** Temurin JDK 25, Spring Boot 4.1.1 and Maven 3.10.0 pinned through the official script-only Maven Wrapper 3.3.4. Pin the Maven distribution SHA-256. Install the ARM64 JDK under the user's JavaVirtualMachines directory without changing shell profiles.

**Reasoning:** follows the approved stack, uses conventional Java tooling and makes the Maven version explicit. The Spring Boot parent manages compatible framework dependency versions.

**Trade-offs:** a JDK remains a local prerequisite; first-use dependency downloads need network access. Wrapper pinning is not a complete dependency lockfile. A user-level JDK still needs selecting in terminals and IDEs.

## 2026-10-21 — Small checkpoints and minimal initial dependencies

**Context:** working code alone does not meet the learning goal.

**Options:** generate the entire milestone together, or introduce startup, persistence and tests in separate reviewable steps.

**Choice:** implement checkpoint 1 with only Web MVC, then pause for review. Add JDBC, Flyway, Actuator and testing dependencies in the approved later checkpoints. No Lombok, code generators or empty business layers.

**Reasoning:** keeps each new concept tied to code Roope can inspect and explain.

**Trade-offs:** the first build has no tests, database or health endpoint. HTTP 404 is the expected startup smoke-check result until routes are introduced.

## 2026-10-21 — Backend directory and local network binding

**Context:** the final application will have separate Java and React builds; the current server is unfinished.

**Options:** root-level Maven project or separate backend; bind all interfaces or loopback.

**Choice:** `backend/` with package `com.punttiurpo`, reserving `frontend/` for Milestone 5. Default to loopback and configurable port 8080.

**Reasoning:** keeps build boundaries clear and local development contained.

**Trade-offs:** commands must run from the correct directory; container deployment will need an explicit network binding override.

## 2026-10-21 — Database ownership (approved, not yet implemented)

**Context:** future relational features need predictable schema management and realistic tests.

**Options:** Hibernate-generated schema or Flyway migrations; an in-memory substitute or real PostgreSQL for persistence tests.

**Choice:** PostgreSQL 18.6, Flyway-owned schema and Testcontainers integration tests. Introduce the database in checkpoint 2 and domain schema in Milestone 2. Hibernate will validate rather than modify the schema when added.

**Reasoning:** makes schema history explicit and tests PostgreSQL behavior directly.

**Trade-offs:** requires Docker for database integration tests and manual review of migration changes.

**2026-10-22 update:** implemented for Milestone 1. The first domain migration remains Milestone 2 work; only Flyway's schema-history table exists now.

## 2026-10-22 — Build full milestones before tutorial review

**Context:** Roope learns better by examining a complete working milestone before studying its tutorial.

**Options:** pause after each small internal checkpoint, or finish and verify the approved milestone first.

**Choice:** finish complete milestones before tutorial review, while retaining explicit approval between milestones.

**Reasoning:** follows Roope's revised preference. Documentation must still be developed alongside implementation and cite real source.

**Trade-offs:** each review covers more code. Small source examples and progressive exercises keep the tutorial approachable; exercises do not block approved implementation.

## 2026-10-22 — Framework health and real database integration tests

**Context:** we need observable database availability without premature domain endpoints.

**Options:** a custom health controller or Actuator; mock database status or test a real PostgreSQL outage.

**Choice:** standard Actuator aggregate health, only the health endpoint exposed, component details hidden and probe groups disabled. A Testcontainers PostgreSQL instance backs full HTTP integration tests. Pause/unpause that disposable container to preserve its port during a simulated outage; always unpause in `finally`. Test setup supplies explicit JDBC URL parameters through Testcontainers' API.

**Reasoning:** verifies actual Spring configuration, network requests, JDBC timeouts and recovery. Exact minimal response assertions caught probe-group metadata during the first run; explicit probe configuration resolves that mismatch.

**Trade-offs:** tests require Docker and take longer than mocks. Outage tests intentionally emit private diagnostic warnings. Spring Boot Test's unused Mockito dependencies are excluded to avoid unnecessary Java-agent attachment; add mocks only when justified. Initial database failure prevents startup because Flyway requires a connection; later failure is observable as HTTP 503.

## 2026-10-22 — Local environment and PostgreSQL storage

**Context:** Java runs on macOS while PostgreSQL runs in Docker; configuration must agree without sharing other applications' resources.

**Options:** hard-coded credentials, custom dotenv loading, or standard environment variables exported from an ignored file.

**Choice:** ignored `.env` with a generated local password and documented shell export; `.env.example` contains a blank required password. Compose binds port 54328 to loopback and mounts its own volume at `/var/lib/postgresql`, the PostgreSQL 18 image's parent data location.

**Reasoning:** uses standard Spring and Compose behavior without a dotenv library or custom startup abstraction. Tests supply independent credentials and random ports. Flyway's PostgreSQL module is included because its database support is separate from core.

**Trade-offs:** `.env` must be exported before starting Java. Existing-volume credentials do not change merely because environment initialization values change. Local container environment storage is not a production secret-management solution. Stop/start persistence does not prove backup or restore readiness.
