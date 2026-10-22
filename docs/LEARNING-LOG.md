# Learning log

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
