# Operation and deployment

## Local operation

The [README](../README.md) provides the copyable setup, environment export, Maven and executable-JAR commands. Requirements are JDK 25 and running Docker Desktop; global Maven is unnecessary.

Local defaults: Java binds to `127.0.0.1:8080`; PostgreSQL binds to `127.0.0.1:54328`. The HTTP port and database settings are environment-configurable. Compose is a database-only local stack with a dedicated volume, health check and `unless-stopped` restart policy. No production Dockerfile exists yet. Workout APIs are available only after restarting the Java process with the updated build; refresh Swagger in a normal browser afterward. VS Code's in-app browser has prevented request editing in local testing.

Database lifecycle commands, from the repository root:

```sh
docker compose up -d --wait db
docker compose ps
docker compose logs --tail 50 db
docker compose stop db
```

Start Java only after the database is healthy. Ctrl+C stops a foreground Java process. Stopping the database preserves its named volume. No secrets need to be printed to inspect status.

For an optional health outage exercise, start Java and check `/actuator/health`, stop only this project's database with `docker compose stop db`, check for 503, and bring it back with `docker compose up -d --wait db`. The same Java process should report 200 again. Use only this project directory, not another Compose stack.

## Troubleshooting

- Java missing or wrong version: select JDK 25 with `JAVA_HOME`, put its `bin` first in `PATH`, and verify `./mvnw --version`.
- Port occupied: change the appropriate `.env` port, export settings again and rerun Compose/Java; do not stop unrelated services.
- Password missing: set it in ignored `.env` and export the file before launching Java. Compose loading it does not make it available to a separate shell process.
- Authentication failure after editing `.env`: existing-volume credentials do not change automatically. Restore the correct settings or change the database role deliberately; do not delete the volume to bypass diagnosis.
- Flyway migration/validation failure: inspect the error and applied versions; Milestone 3 has V1–V4. Do not reset the volume or edit an applied migration to bypass diagnosis. Hibernate validates the migrated schema.
- Startup fails with a database error: check `docker compose ps`, credentials and ports. Flyway intentionally prevents startup against an inaccessible database.
- HTTP 404 at `/`: expected. Check `/actuator/health` for operational health.
- Health 503 after startup: inspect local logs and database state. Connection, validation and socket timeouts bound JDBC waits.
- Testcontainers cannot find Docker: start Docker Desktop and ensure your Docker context is usable. Do not skip the tests or substitute an in-memory database.
- Maven checksum mismatch: investigate the pinned URL/distribution; do not disable verification.

## Logs and secrets

Java writes ECS JSON logs to the terminal and PostgreSQL logs are available through Compose. Local API documentation is available at `/swagger-ui/index.html`, with OpenAPI JSON at `/v3/api-docs`. These expose write operations without authentication and must remain local-only until the security milestone. Dependency outages produce expected server-side diagnostic warnings, which can include stack traces. HTTP health and error responses do not expose those details. Never publish raw logs without reviewing them for sensitive information.

The implementation created an ignored local `.env` with a random password and file permissions 0600. `.env.example` contains no password. Environment configuration is adequate for this local milestone; production secret handling must be reviewed before deployment.

## Future deployment: not implemented or authorized

Target: `puntti-urpo.iltapaska.com` on the existing Debian Intel NUC. Development uses ARM64 macOS; production images must support AMD64 Linux. Inspect existing services, Docker volumes, ports, memory, nginx and Cloudflare routing in Milestone 7 before proposing changes.

Use an independent Compose stack, private database networking, dedicated persistent storage, multi-stage builds, health checks, restart policies and appropriate resource limits. Production container networking needs an explicit binding override from the local loopback default. Do not copy local host-port exposure into production unquestioningly.

Authentication and separation of public demo data from private workout history are required before public access. Keep secrets outside Git. No production Docker build, nginx configuration, Cloudflare route, backup job or tested restore procedure exists yet. Local volume persistence is not a backup. Design and test backup restoration before production deployment.

Do not reuse other applications' volumes or modify existing server configurations without approval. Deployment requires explicit authorization.
