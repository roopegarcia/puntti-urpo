# Puntti-Urpo

A Finnish strength-training logbook being built for daily use, Java learning and software developer job applications. The goal is to understand and explain the implementation as well as deliver working software.

## Current status

**Milestone 3: Workout domain implemented; validation evidence is in the learning log.** The local REST API supports the ten requested built-in exercises, custom exercises, workout creation/history summaries, ordered logged exercises and draft/completed sets. Workouts, logged exercises and sets can be read, edited and deleted with explicit cascade boundaries. No frontend, authentication, progression statistics, RIR or estimated 1RM calculations exist yet.

Build and verify a complete milestone, then review the working code and tutorial. Stop for approval between milestones; tutorial exercises do not block implementation within an approved milestone.

## Stack and structure

- Eclipse Temurin JDK 25; locally installed ARM64 runtime: 25.0.4.1.
- Spring Boot 4.1.1, Spring Web MVC, embedded Tomcat and Actuator.
- Spring Data JPA/Hibernate, Bean Validation, JDBC/HikariCP, PostgreSQL 18.6 and Flyway.
- Springdoc OpenAPI / Swagger UI 3.1.1.
- Maven 3.10.0 through Maven Wrapper 3.3.4.
- JUnit and Spring Boot Test with Testcontainers PostgreSQL integration tests.
- Planned frontend: React, TypeScript and Vite.

`backend/` is the Maven project, with base package `com.punttiurpo`. Controller, service, repository, entity, DTO, mapper and exception packages implement the catalogue. `compose.yaml` defines only this project's local database. The frontend belongs to Milestone 5.

## Built-in catalogue

- Barbell Back Squat
- Romanian Deadlift
- Bulgarian Split Squat
- Deadlift
- Bench Press
- Dumbbell Curl
- Skull Crusher
- Dumbbell Lateral Raise
- Cable Flys
- Cable Tricep. Extension

Names follow Roope's requested English labels; product UI and validation instructions remain Finnish. Custom exercises remain available and are not removed by the catalogue migration.

## Local setup (macOS)

Install Eclipse Temurin JDK 25 for your architecture. On this Mac the ARM64 JDK lives under `~/Library/Java/JavaVirtualMachines/`. Docker Desktop must be running. Global Maven is unnecessary.

From the repository root, create local configuration once:

```sh
cp .env.example .env
chmod 600 .env
```

Edit `.env` and set a nonempty `POSTGRES_PASSWORD`. Use a local development password; do not commit it. The example intentionally has no password, and Compose refuses to start without one. A generated local `.env` was already created on the implementation machine; preserve it instead of copying over it.

Start the database and export the same settings into your shell:

```sh
docker compose up -d --wait db
set -a
source .env
set +a
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend
./mvnw --version
./mvnw spring-boot:run
```

Compose reads `.env` automatically; Java does not. Sourcing it is a separate step. Keep `.env` as trusted shell assignments; quote values containing shell-special characters. Docker stores container environment configuration locally, so this is development setup, not production secret management.

In another terminal:

```sh
curl -i http://127.0.0.1:8080/actuator/health
```

Expect HTTP 200 and `{"status":"UP"}`. `/` still returns 404 because no product route exists. The server and database ports bind to loopback. Stop Java with Ctrl+C. From the repository root, stop the database with `docker compose stop db`; its named volume remains intact.

If a port is occupied, change `DB_PORT` or `SERVER_PORT` in `.env`, export the file again and rerun the relevant commands. Defaults are 54328 for PostgreSQL and 8080 for HTTP. Never stop unrelated services to free a port.

## Build and tests

From `backend/`, with JDK 25 selected and Docker running:

```sh
./mvnw verify
```

This compiles, runs unit tests and real PostgreSQL integration tests, and packages an executable JAR. Tests supply their own credentials and random ports; neither `.env` nor the Compose database is needed for tests. They fail if Docker is unavailable rather than silently skipping database coverage. The outage test pauses only its own disposable database container and restores it in a `finally` block. Workout deletion tests operate only on their own test databases.

The wrapper downloads Maven and verifies its pinned SHA-256. Initial dependency and Docker-image downloads require network access. Windows uses `mvnw.cmd` and an appropriate JDK 25/environment setup.

To run the packaged application, start Compose and export `.env` as above, then from `backend/`:

```sh
java -jar target/puntti-urpo-0.0.1-SNAPSHOT.jar
```

See [the learning log](docs/LEARNING-LOG.md) for actual results and [the Java tutorial](docs/JAVA-FOR-ROOPE.md) for a guided explanation.

## Database and limitations

Flyway owns the schema: V1 creates `exercise`, V2 records the original seed history, V3 switches the built-in catalogue to the ten requested English exercise names, and V4 creates workouts, logged exercises and sets. Equivalent catalogue IDs and custom exercises are preserved. Hibernate validates the schema and never generates it. Existing volumes upgrade through versioned migrations without a reset. Custom exercises are editable; built-ins are read-only. Catalogue exercise deletion remains unexposed and database-restricted while referenced. Deleting a workout cascades to its logged exercises/sets; deleting a logged exercise cascades only to its sets. Catalogue records and other workouts are preserved.

The Compose database has its own `puntti-urpo_postgres_data` volume. Stop/start and `docker compose down` preserve it; **`docker compose down -v` deletes it**. PostgreSQL credentials/database initialization settings apply when the volume is first initialized. Editing those settings later does not update existing roles or data. See [database documentation](docs/DATABASE.md).

Use `/api/exercises?q=bench` for search, `/api/workouts` for workout summaries, `/v3/api-docs` for OpenAPI JSON and `/swagger-ui/index.html` for interactive API documentation. See [API documentation](docs/API.md) for POST/PUT/DELETE payloads, a copyable workout logging sequence, pagination, errors and limits. Write operations are local-only and have no per-user ownership yet.

The health endpoint reports dependency availability, not workout correctness, authentication or backup readiness. A database failure during startup prevents the application from starting because Flyway requires a connection; a later failure returns HTTP 503 while the process remains alive.

## Deployment

Local only. Authentication, isolated NUC deployment, nginx, Cloudflare routing, production backups and restore testing are Milestone 7 work. The NUC must be inspected first, and deployment requires explicit authorization. Personal workout records must never be exposed in a public portfolio demo without approval.

## Documentation

- [Java for Roope](docs/JAVA-FOR-ROOPE.md): progressive tutorial using actual source.
- [Architecture](docs/ARCHITECTURE.md): components, request flow and planned layers.
- [Database](docs/DATABASE.md): storage, migrations and future modeling.
- [API](docs/API.md): current HTTP contract.
- [Deployment](docs/DEPLOYMENT.md): local operation and deployment constraints.
- [Decisions](docs/DECISIONS.md): options, reasoning and trade-offs.
- [Learning log](docs/LEARNING-LOG.md): milestone evidence and learning questions.
- [Changelog](CHANGELOG.md): implemented changes.

The repository is private. Portfolio source review must exclude credentials and personal data. Professional presentation and interview-review instructions belong to Milestone 8.
