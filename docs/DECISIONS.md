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

## 2026-10-22 — Exercise model, catalogue origin and write contract

**Context:** Milestone 2 needs a useful exercise catalogue before workout relationships exist.

**Options:** fixed enum taxonomies or bounded free text; editable seed records or protected catalogue entries; patch updates or full replacement.

**Choice:** a single Exercise table with identity ID, required normalized name (100), optional category/muscle/equipment (50 each) and notes (2000). Metadata remains Finnish-friendly free text. API-created records are custom; eight versioned Finnish seeds are read-only through the service. PUT replaces editable fields and clears omitted optional fields. No DELETE endpoint exists yet.

**Reasoning:** keeps the relational model understandable, provides an initial usable catalogue and avoids inventing a training taxonomy or deletion cascades before workout references are designed. DTOs control input/output explicitly.

**Trade-offs:** custom origin is not per-user ownership; authentication is still required before public exposure. Metadata may need normalization later. Concurrent updates use last successful write semantics; optimistic versioning is deferred. No workout relationships or cascades are implied.

## 2026-10-22 — Name integrity, search and pagination

**Context:** duplicate exercise names would make catalogue selection ambiguous, while an unbounded listing would age poorly.

**Options:** application-only uniqueness checks or database enforcement; wildcard LIKE queries or literal substring search; raw Spring Page serialization or an explicit envelope.

**Choice:** PostgreSQL unique index on `lower(name)` across both origins. SQL CHECKs enforce nonblank/trimmed ordinary-space names, and request normalization/validation handles Java whitespace and text limits. JPQL `locate` provides literal case-insensitive name search with bound parameters. Use deterministic `lower(name), id` ordering and explicit items/page/size/total metadata. Default size 20, max 100; page 0–1,000,000 avoids Hibernate integer-offset overflow.

**Reasoning:** the database closes races between concurrent writers; substring search does not turn `%` or `_` into unexpected wildcards. A small explicit API envelope avoids coupling the contract to Spring's internal Page serialization.

**Trade-offs:** uniqueness is not accent-insensitive; sorting follows database collation, not a guaranteed Finnish dictionary order. Substring scans are adequate for this small catalogue; add an appropriate index only when measured need justifies it. Integrity failures use a safe generic 409 instead of exposing SQL constraint internals.

## 2026-10-22 — JPA service transactions, errors and API discovery

**Context:** the catalogue introduces the first business persistence layer and HTTP write operations.

**Options:** expose entities directly or use DTOs; allow Hibernate schema updates or Flyway ownership; custom exception JSON or Spring ProblemDetail.

**Choice:** constructor-injected controller/service/repository layers, explicit mapper, request/response records, Flyway V1/V2, Hibernate validate and OSIV disabled. Read-only service defaults and explicit write transactions. Flush within create/update surfaces constraint conflicts, with rollback preserving previous data. Use centralized ProblemDetail advice and ECS JSON console logs. Add Springdoc 3.1.1, compatible with Spring Boot 4, and test `/v3/api-docs` plus Swagger UI.

**Reasoning:** demonstrates conventional Java/Spring boundaries without a base CRUD framework or mapping/code-generation dependency. Separate DTOs prevent accidental entity exposure; framework errors remain consistent.

**Trade-offs:** there is more explicit mapping code and more dependencies, each with a concrete purpose: JPA persistence, Bean Validation, OpenAPI and Mockito for focused service tests. Mockito's subclass mock maker avoids Java-agent attachment but does not support final-class mocking. Full PostgreSQL integration tests remain necessary for persistence claims.

Compatibility references: [Spring SQL/JPA documentation](https://docs.spring.io/spring-boot/reference/data/sql.html), [Springdoc documentation](https://springdoc.org/).

## 2026-10-22 — User-requested ten-exercise catalogue

**Context:** Roope only needs ten named exercises for now and explicitly requested English labels.

**Options:** edit the already-applied seed migration, reset the database, or add a migration preserving user data and equivalent exercise IDs.

**Choice:** add V3. Rename four semantically equivalent built-ins in place, remove four unused built-ins before workout references exist, and insert six requested entries. Keep every custom row unchanged. Preserve the exact labels, including `Cable Flys` and `Cable Tricep. Extension`. Metadata remains Finnish. Unspecified equipment is left null rather than prescribing a particular implementation of an exercise.

**Reasoning:** existing databases and fresh installs follow the same immutable migration history. Exercise labels are user content, so English names do not change the Finnish UI direction.

**Trade-offs:** a collision with an existing custom name fails safely rather than merging, deleting or converting user data. The original V2 seeds remain historical migrations. A real PostgreSQL upgrade test verifies custom notes/IDs and the complete requested name set.

## 2026-10-22 — Workout relationships and explicit deletion

**Context:** Milestone 3 introduces workout CRUD and completed/draft set storage; relationships and delete behavior must be defined first.

**Options:** bidirectional entity collections with ORM cascade/orphan removal, or required child-to-parent relationships with database cascades.

**Choice:** WorkoutExercise has required lazy references to Workout and Exercise; WorkoutSet has a required lazy reference to WorkoutExercise. Use unidirectional `@ManyToOne` mappings, no JPA delete cascades/parent collections, and explicit SQL cascades from workout → entries → sets. Catalogue references use RESTRICT. No catalogue deletion endpoint is added. One catalogue exercise may appear repeatedly in a workout at different positions.

**Reasoning:** the schema expresses ownership directly and PostgreSQL can remove children reliably without loading an entity collection. The same rules hold for direct SQL and API operations.

**Trade-offs:** nested response assembly is explicit. Workout details fetch entries with an entity graph and all sets once, then group by entry ID. There is no ORM orphan-removal convenience. Deletes are permanent and have no undo. Future authentication must check ownership before exposing these operations publicly.

## 2026-10-22 — Exact set values, drafts and timestamps

**Context:** logging needs incomplete next sets, exact kg/RPE storage, and optional session times before strength calculations exist.

**Options:** require completed values on every set creation or allow drafts; floating-point types or BigDecimal; date-only values or implicit server-local timestamps.

**Choice:** drafts can omit weight/repetitions; completed sets require nonnegative weight and 1–1000 repetitions. Weight uses BigDecimal/numeric(6,2); RPE uses BigDecimal/numeric(3,1), 6–10 in half steps. API validation rejects excess precision. WARMUP/WORKING is a string-persisted enum, default WORKING. Optional input completion uses Boolean with an explicit false default, while persisted/response state is non-null boolean. Date is explicit LocalDate; optional start/end are Instant with validated chronological order.

**Reasoning:** avoids binary precision errors and supports preparing a set before recording performance. A strict Jackson 3 record-deserialization test caught the omitted primitive completion flag; nullable input plus normalization makes the documented empty draft body work. A training date must not shift with server timezone.

**Trade-offs:** kg represents the entered load; per-hand dumbbell multiplication is not inferred. PostgreSQL timestamp precision is microseconds. Optional input null/default handling is explicit, not global Jackson relaxation. RPE is stored subjective effort only; RIR/e1RM calculations remain Milestone 4. No new dependencies are needed beyond the existing stack.

## 2026-10-22 — Ordering, replacement APIs and scope

**Context:** exercise/set order matters during logging, and CRUD must remain understandable before a frontend exists.

**Options:** renumber all siblings on every delete/move, introduce atomic batch reorder, or use explicit unique positions with stable gaps.

**Choice:** zero-based unique positions per parent (0–1,000,000), append at max + 1, preserve gaps after deletion. PUT replaces metadata/performance fields. Omitted set order preserves the position on updates, while optional performance fields clear and default type/completion are reapplied. Moving to an occupied position returns 409 and rolls back. Use GET endpoints matching creation Location headers. Workout summaries are paginated; detail includes ordered children. Swagger explicitly documents 201/204 response statuses.

**Reasoning:** supports logging and editing without an arbitrary general CRUD abstraction or a premature reorder protocol. Full replacement follows the exercise API convention.

**Trade-offs:** concurrent appends can conflict and require a client retry; there is no atomic swap/reorder-all endpoint, optimistic locking or soft delete. Catalogue reference changes require removing/re-adding a logged entry. No new formula, progression chart, frontend or deployment is included.
