# Learning log

## 2026-10-22 — Milestone 3 complete: workout domain and exact requested catalogue

**Authorization:** Roope requested the ten exact English exercise names and explicitly selected “Update catalogue and implement Milestone 3.” The normal browser resolved Swagger request editing; use a normal browser instead of the VS Code in-app browser for these exercises. Stop before Milestone 4 until approved.

**Built:** V3 replaces built-ins with the requested ten names without editing applied migrations, preserving four equivalent IDs and custom records. V4 defines Workout, WorkoutExercise and WorkoutSet, required foreign keys, unique sibling orders, exact decimal storage and explicit deletion behavior. Added DTOs, mapping, repositories, transaction-scoped services and read/create/replace/delete APIs, nested detail and paginated workout summaries. Logged exercise references remain fixed; catalogue records are never deleted by workout operations.

**Model rules established before implementation:** required child-to-parent references; workout → logged exercise → set ownership cascades in PostgreSQL; catalogue reference deletion RESTRICT; unique zero-based positions with valid gaps. BigDecimal/numeric store kg and RPE. Draft sets may be incomplete, completed sets require load and positive reps. Date is LocalDate, optional timestamps are Instant with end >= start. RPE is recorded/validated only, with RIR/e1RM deferred to Milestone 4.

**New Java concepts:** lazy ManyToOne associations, foreign keys versus ORM cascades, enum text persistence, BigDecimal precision/scale/remainder, nullable input Boolean versus persisted boolean, explicit draft defaults, LocalDate versus Instant, nested records, grouping collectors/maps and query-aware response assembly. Tutorial chapters 23–30 use actual source. No new dependencies were needed.

**What confused Roope:** the VS Code in-app browser prevented Swagger editing; a normal browser solved it. No Java understanding is assumed or claimed. The complete working milestone is available before tutorial study, per Roope's preference.

**Actual verification:**

- `./mvnw -B -ntp verify`: BUILD SUCCESS. 29 tests, zero failures/errors/skips across seven classes: eight unit and 21 PostgreSQL/integration tests.
- Upgrade-path test applied V1/V2, added custom data, applied V3/V4, and checked the complete exact ten-name set, custom notes/ID preservation, equivalent Bench Press ID and repeat migration without duplication.
- Workout HTTP/PostgreSQL tests covered metadata and nested CRUD, draft completion, precision/bounds, RPE steps, time ranges, missing parents/resources, pagination, order conflict rollback, delete isolation and catalogue foreign-key restriction.
- Direct SQL tests rejected invalid completed sets, RPE steps and missing parent references. Foundation tests now validate four migrations/five tables and retain database outage/recovery coverage.
- The first draft tests detected strict primitive-argument deserialization for omitted completion. Optional input Boolean with an explicit false default fixed empty draft bodies without changing global JSON parsing.
- Swagger tests verify workout/entry paths and explicitly documented 201 creation/204 deletion statuses. Existing catalogue tests were updated to the requested names and ten built-ins.
- Packaged JAR upgraded the existing local volume from two applied migrations to four, without a reset. A fingerprint comparison verified every custom exercise row/value was unchanged; the equivalent Bench Press ID was preserved.
- Packaged-JAR workout creation, exercise append, completed set persistence, nested detail and OpenAPI passed. A restart preserved the recorded decimals and child IDs. Deleting only the disposable verification workout removed its descendants and retained catalogue data.
- Removed only that verification workout. Stopped only the independently launched verification JAR. Left the user's pre-existing application/database running and the volume intact. Restart the user-run Java app to load the new endpoints.
- Final `git diff --check`, authored whitespace, local Markdown links, XML and credential-exclusion checks passed. Seven test report XML files independently confirmed all 29 tests passed. No production/NUC/Cloudflare changes were made.

**Added files in this milestone:**

- `backend/src/main/java/com/punttiurpo/controller/WorkoutController.java`
- `backend/src/main/java/com/punttiurpo/controller/WorkoutEntryController.java`
- `backend/src/main/java/com/punttiurpo/dto/AddWorkoutExerciseRequest.java`
- `backend/src/main/java/com/punttiurpo/dto/WorkoutExerciseRequest.java`
- `backend/src/main/java/com/punttiurpo/dto/WorkoutExerciseResponse.java`
- `backend/src/main/java/com/punttiurpo/dto/WorkoutPage.java`
- `backend/src/main/java/com/punttiurpo/dto/WorkoutRequest.java`
- `backend/src/main/java/com/punttiurpo/dto/WorkoutResponse.java`
- `backend/src/main/java/com/punttiurpo/dto/WorkoutSetRequest.java`
- `backend/src/main/java/com/punttiurpo/dto/WorkoutSetResponse.java`
- `backend/src/main/java/com/punttiurpo/dto/WorkoutSummaryResponse.java`
- `backend/src/main/java/com/punttiurpo/entity/Workout.java`
- `backend/src/main/java/com/punttiurpo/entity/WorkoutExercise.java`
- `backend/src/main/java/com/punttiurpo/entity/WorkoutSet.java`
- `backend/src/main/java/com/punttiurpo/entity/WorkoutSetType.java`
- `backend/src/main/java/com/punttiurpo/exception/InvalidWorkoutException.java`
- `backend/src/main/java/com/punttiurpo/exception/WorkoutNotFoundException.java`
- `backend/src/main/java/com/punttiurpo/mapper/WorkoutMapper.java`
- `backend/src/main/java/com/punttiurpo/repository/WorkoutExerciseRepository.java`
- `backend/src/main/java/com/punttiurpo/repository/WorkoutRepository.java`
- `backend/src/main/java/com/punttiurpo/repository/WorkoutSetRepository.java`
- `backend/src/main/java/com/punttiurpo/service/WorkoutService.java`
- `backend/src/main/resources/db/migration/V3__use_requested_exercise_catalogue.sql`
- `backend/src/main/resources/db/migration/V4__create_workout_domain.sql`
- `backend/src/test/java/com/punttiurpo/CatalogueMigrationIntegrationTest.java`
- `backend/src/test/java/com/punttiurpo/WorkoutApiIntegrationTest.java`
- `backend/src/test/java/com/punttiurpo/service/WorkoutServiceTest.java`

**Updated files in this milestone:**

- `AGENTS.md`
- `CHANGELOG.md`
- `README.md`
- `backend/src/main/java/com/punttiurpo/controller/ExerciseController.java`
- `backend/src/main/java/com/punttiurpo/exception/ApiExceptionHandler.java`
- `backend/src/main/resources/db/migration/README.md`
- `backend/src/test/java/com/punttiurpo/ExerciseApiIntegrationTest.java`
- `backend/src/test/java/com/punttiurpo/FoundationIntegrationTest.java`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/DATABASE.md`
- `docs/DECISIONS.md`
- `docs/DEPLOYMENT.md`
- `docs/JAVA-FOR-ROOPE.md`
- `docs/LEARNING-LOG.md`

**Git state:** Milestone 1 remains local commit `34a5e14`. Milestones 2 and 3 changes remain uncommitted. No push or deployment occurred.

**What Roope should be able to explain after studying:** catalogue versus logged-exercise identity, ownership and database deletion rules, exact decimal versus binary floating point, draft/default semantics, date/instant differences, and how nested detail avoids a query per child.

**Three questions for later:**

1. Why do logged exercises reference both a workout and a reusable catalogue exercise, while sets reference the logged occurrence?
2. How do SQL ON DELETE CASCADE and RESTRICT protect ownership, and why are no ORM delete cascades used here?
3. Why use BigDecimal and an optional input Boolean instead of double and an omitted primitive flag?

**Suggested exercise:** create a disposable workout in Swagger, add two requested exercises, prepare/complete a draft set, inspect nested detail and provoke an occupied-order conflict. Delete only that disposable workout and verify the catalogue remains. See API.md for the exact request sequence and Java tutorial chapters 23–30 for the explanation.

**Review state:** Milestone 3 implemented and verified. Stop before Milestone 4 (RPE/RIR/e1RM calculations) until approved.

---

## 2026-10-22 — Milestone 2 complete: exercise catalogue

**Authorization and Git:** Roope approved committing Milestone 1 and proceeding to Milestone 2. Milestone 1 is local commit `34a5e14` (`Complete milestone 1 Java and PostgreSQL foundation`). Milestone 2 changes remain uncommitted for review. Nothing was pushed or deployed.

**Built:** an Exercise JPA model, Flyway V1/V2 with eight Finnish built-ins, literal case-insensitive name search, bounded pagination, read/create/replace REST operations, server-controlled custom origin and read-only built-in protection. Constructor-injected layers, explicit mapper/DTO records, Bean Validation, ProblemDetail errors, transaction rollback, OpenAPI/Swagger and structured ECS logs are implemented. No deletion API, workout relationships, authentication or frontend was added.

**New concepts:** constructors/access modifiers, JPA mapping, identity IDs, interfaces/generics, runtime repository proxies, Optional, composition and constructor injection, records, compact constructors, validation, transaction proxies, managed entities/dirty checking, flush versus commit, rollback, method references/streams, local type inference, domain exceptions and safe HTTP translation. Tutorial chapters 14–22 use the actual milestone source.

**What confused Roope:** not yet discussed. No understanding is assumed or claimed. Review the working milestone and tutorial at Roope's preferred pace.

**Actual validation:**

- `./mvnw -B -ntp verify`: BUILD SUCCESS; 16 tests, zero failures/errors/skips. Five unit tests and eleven full-application integration tests across four classes.
- Unit tests checked whitespace normalization, blank/length boundaries, missing-record failures, built-in edit protection and custom full-replacement behavior.
- Catalogue HTTP/PostgreSQL tests checked seeds, pagination, case-insensitive literal search, create/read/replace persistence, duplicate conflicts, transaction rollback, built-in protection, unsupported deletion, malformed JSON, invalid IDs/query/pagination, direct SQL constraints and OpenAPI/Swagger compatibility.
- The three foundation tests still verify PostgreSQL connectivity, actual migration/schema state and 200 → 503 → 200 health behavior during a test database pause/recovery.
- Added an upper page bound and tested extreme input to avoid Hibernate offset overflow; this is documented in the API contract.
- Packaged-JAR smoke test upgraded the existing Compose volume from zero applied migrations to V1/V2, without resetting or deleting the volume. Eight seeds appeared and Hibernate schema validation succeeded.
- Packaged-JAR search, custom creation/replacement, duplicate rollback and OpenAPI access passed. Restarting the JAR preserved the custom record and did not duplicate seeds or migration entries.
- Removed only the smoke-test custom row by its exact generated ID after verification. Application and database are stopped; the existing named volume is preserved.
- `git diff --check`, authored-file whitespace, local Markdown links and POM XML checks passed. The ignored local password did not appear in Git-visible files or validation logs.

**Changed tracked files:**

- `AGENTS.md`
- `CHANGELOG.md`
- `README.md`
- `backend/pom.xml`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/db/migration/README.md`
- `backend/src/test/java/com/punttiurpo/FoundationIntegrationTest.java`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/DATABASE.md`
- `docs/DECISIONS.md`
- `docs/DEPLOYMENT.md`
- `docs/JAVA-FOR-ROOPE.md`
- `docs/LEARNING-LOG.md`

**Added files:**

- `backend/src/main/java/com/punttiurpo/controller/ExerciseController.java`
- `backend/src/main/java/com/punttiurpo/dto/ExercisePage.java`
- `backend/src/main/java/com/punttiurpo/dto/ExerciseRequest.java`
- `backend/src/main/java/com/punttiurpo/dto/ExerciseResponse.java`
- `backend/src/main/java/com/punttiurpo/entity/Exercise.java`
- `backend/src/main/java/com/punttiurpo/exception/ApiExceptionHandler.java`
- `backend/src/main/java/com/punttiurpo/exception/BuiltInExerciseException.java`
- `backend/src/main/java/com/punttiurpo/exception/ExerciseNotFoundException.java`
- `backend/src/main/java/com/punttiurpo/mapper/ExerciseMapper.java`
- `backend/src/main/java/com/punttiurpo/repository/ExerciseRepository.java`
- `backend/src/main/java/com/punttiurpo/service/ExerciseService.java`
- `backend/src/main/resources/db/migration/V1__create_exercise.sql`
- `backend/src/main/resources/db/migration/V2__seed_exercise_catalogue.sql`
- `backend/src/test/java/com/punttiurpo/ExerciseApiIntegrationTest.java`
- `backend/src/test/java/com/punttiurpo/dto/ExerciseRequestTest.java`
- `backend/src/test/java/com/punttiurpo/service/ExerciseServiceTest.java`
- `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`

**What Roope should be able to explain after studying:** how HTTP requests pass through the layers, why entities and DTOs differ, what Spring implements for a repository interface, where validation/uniqueness are enforced, how a managed update rolls back on conflict, and which tests establish real persistence behavior.

**Three questions for later:**

1. Why use a mutable JPA entity internally but immutable records at the HTTP boundary?
2. How does constructor injection allow a service unit test without Spring, while integration tests still exercise real PostgreSQL?
3. Why does a duplicate-name update preserve the previous database values even after the Java entity was modified?

**Suggested exercise:** use Swagger UI to create a custom exercise, search for it, replace its fields and provoke a duplicate-name 409. Trace both requests through the controller, service, repository, SQL constraint and exception advice. Tutorial study does not block completion of an approved milestone.

**Review state:** Milestone 2 implemented and verified. Stop before Milestone 3 (Workout domain) until approved; define relationships, precision, ordering and deletion rules before that implementation.

---

## 2026-10-22 — Milestone 1 complete: Java foundation

**Workflow:** Roope requested complete milestones before tutorial review. This supersedes the original checkpoint review gate; exercises are for later study. Milestone 2 still requires approval.

**Built:** dedicated PostgreSQL 18.6 Compose stack and persistent volume; ignored generated local credentials; JDBC/HikariCP configuration; Flyway initialization; Actuator health; and three full-application PostgreSQL integration tests. No domain schema, JPA entities, authentication or frontend was introduced.

**Concepts added:** environment/property resolution, JDBC and connection pooling, schema migration history, Spring-managed test dependencies, random HTTP ports, dynamic test properties, method references, generics, checked exceptions, `finally`, and dependency-aware health. These are explained using actual code in `JAVA-FOR-ROOPE.md`.

**What confused Roope:** not yet discussed. No claim is made that Roope has demonstrated understanding.

**Actual verification:**

- `./mvnw -B -ntp verify`: BUILD SUCCESS. Three tests, zero failures, zero errors, zero skipped. Full application tests connected to real PostgreSQL 18.6 through Testcontainers.
- SQL connectivity test verified the PostgreSQL version. Flyway test verified zero applied/pending migrations and only `flyway_schema_history` in the public schema.
- HTTP integration test verified exact 200 `{"status":"UP"}`, 503 `{"status":"DOWN"}` during a test-container pause, then 200 after recovery. No database components or exception details appeared in those bodies.
- The first test run caught probe-group metadata in the health body; explicit probe configuration fixed the contract. Unused Mockito dependencies were excluded to avoid unnecessary Java-agent attachment. Test URL parameters now use Testcontainers' API.
- `docker compose config --quiet` passed. The database initialized successfully with its own project network and volume and loopback port 54328.
- The packaged JAR started against the Compose database on an environment-selected HTTP port. `lsof` confirmed loopback binding; HTTP health changed 200 → 503 → 200 when only this project's database was stopped and restored.
- A temporary verification schema/table held a marker across database stop/start. The marker survived; the entire verification schema was removed afterward. Only Flyway's history table remains in the public schema.
- Local documentation links, authored-file whitespace and POM XML checks passed. `.env` is ignored, has permissions 0600, and its generated password was absent from Git-visible files and validation logs.
- No NUC services, Cloudflare settings or unrelated Compose stacks were changed. No commits, pushes or deployment were performed.

**Files changed in this completion step:**

- Added `.env.example`, `compose.yaml`, `backend/src/main/resources/db/migration/README.md`, and `backend/src/test/java/com/punttiurpo/FoundationIntegrationTest.java`.
- Updated `backend/pom.xml` and `backend/src/main/resources/application.properties`.
- Updated `AGENTS.md`, `README.md` and `CHANGELOG.md`.
- Updated `docs/ARCHITECTURE.md`, `docs/DATABASE.md`, `docs/API.md`, `docs/DEPLOYMENT.md`, `docs/DECISIONS.md`, `docs/JAVA-FOR-ROOPE.md` and `docs/LEARNING-LOG.md`.
- Created ignored local `.env` with generated credentials. It must never be committed.

**End state:** the application and `puntti-urpo-db-1` are stopped. The named database volume is preserved. Follow the README to start them again. Local volume persistence is verified; production backup/restore is not implemented.

**What Roope should be able to explain after studying:** the source-to-JVM build path, Spring startup and environment configuration, separate application/database processes, dependency-aware health, Flyway's purpose and how test databases are isolated from development data.

**Three questions for later:**

1. What do the JDK, Maven Wrapper and Spring Boot each contribute to startup?
2. Why can health return 503 while Java is alive, but startup fail if PostgreSQL is initially unavailable?
3. How do the tests obtain database credentials and ports without reading your local `.env`?

**Suggested exercise:** run the documented local database outage/recovery sequence, observe 200 → 503 → 200 without restarting Java, and explain the result using the configuration and integration test. No exercise is required before this milestone can be considered implemented.

**Review state:** Milestone 1 implemented and verified; awaiting milestone review/approval before Milestone 2, the exercise catalogue.

---

## 2026-10-21 — Milestone 1, checkpoint 1

**Built:** a minimal Java 25 / Spring Boot 4.1.1 web server, Maven 3.10.0 wrapper, local binding and documentation foundation. Database, health checks and automated tests remain later checkpoints.

**Concepts introduced:** Java source and bytecode, JVM/JDK/runtime distinction, packages, imports, classes, implicit constructors, method signatures, static entry points, annotations, application startup, Maven lifecycle and executable JARs.

**What confused Roope:** not yet discussed. Do not infer confusion or understanding from generated code.

**What Roope should be able to explain after review:** how source reaches the JVM, what the wrapper does, why the server stays running and why HTTP 404 currently confirms reachability.

**Exercise:** start on port 18080 using `SERVER_PORT`, verify HTTP 404 with curl, and stop the app without changing source configuration. Detailed steps and three questions are in [JAVA-FOR-ROOPE.md](JAVA-FOR-ROOPE.md).

**Actual verification:**

- Installed Eclipse Temurin 25.0.4.1+1 (ARM64) under `~/Library/Java/JavaVirtualMachines/`, after verifying the archive SHA-256 against Adoptium's package metadata. No shell profile changes.
- `./mvnw --version`: Maven 3.10.0, Eclipse Adoptium Java 25.0.4.1, aarch64.
- `./mvnw -B verify`: BUILD SUCCESS; compilation and executable JAR packaging succeeded. No automated tests exist or ran at this checkpoint.
- Packaged JAR startup: HTTP 404 from `/` on environment-selected port 59161; response contained no stack trace.
- `./mvnw spring-boot:run`: HTTP 404 from `/` on environment-selected port 59168; response contained no stack trace.
- `lsof` confirmed both runs listened on `127.0.0.1` at their selected ports. Both processes were stopped after verification.
- `javap` confirmed the compiled class contains the default constructor and `public static void main(java.lang.String[])`.

**Files added:** `.gitignore`, `README.md`, `CHANGELOG.md`; backend `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `src/main/java/com/punttiurpo/PunttiUrpoApplication.java`, `src/main/resources/application.properties`; documentation `JAVA-FOR-ROOPE.md`, `ARCHITECTURE.md`, `DATABASE.md`, `API.md`, `DEPLOYMENT.md`, `DECISIONS.md`, `LEARNING-LOG.md`. Updated `AGENTS.md` with the approved checkpoint workflow and current state. No commit, push or deployment was performed.

**Historical review state (2026-10-21):** checkpoint review was pending. Superseded by Roope's 2026-10-22 workflow change and the completed milestone entry above.
