# Operation and deployment

## Local operation

The [README](../README.md) provides the copyable setup, environment export, Maven and executable-JAR commands. Requirements are JDK 25, Node.js >=24 for the frontend, and running Docker Desktop; global Maven is unnecessary.

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
- Flyway migration/validation failure: inspect the error and applied versions; The current schema history has V1–V5; V5 translates built-in names without changing IDs. Do not reset the volume or edit an applied migration to bypass diagnosis. Hibernate validates the migrated schema.
- Startup fails with a database error: check `docker compose ps`, credentials and ports. Flyway intentionally prevents startup against an inaccessible database.
- HTTP 404 at `/`: expected. Check `/actuator/health` for operational health.
- Health 503 after startup: inspect local logs and database state. Connection, validation and socket timeouts bound JDBC waits.
- Testcontainers cannot find Docker: start Docker Desktop and ensure your Docker context is usable. Do not skip the tests or substitute an in-memory database.
- Maven checksum mismatch: investigate the pinned URL/distribution; do not disable verification.

## Logs and secrets

Java writes ECS JSON logs to the terminal and PostgreSQL logs are available through Compose. Local API documentation is available at `/swagger-ui/index.html`, with OpenAPI JSON at `/v3/api-docs`. Swagger and OpenAPI now require the owner role; writes require an owner session and CSRF. Hosting remains local-only until deployment review. Dependency outages produce expected server-side diagnostic warnings, which can include stack traces. HTTP health and error responses do not expose those details. Never publish raw logs without reviewing them for sensitive information.

The implementation created an ignored local `.env` with a random password and file permissions 0600. `.env.example` contains no password. Environment configuration is adequate for this local milestone; production secret handling must be reviewed before deployment.

## Future deployment: not implemented or authorized

Target: `puntti-urpo.iltapaska.com` on the existing Debian Intel NUC. Development uses ARM64 macOS; production images must support AMD64 Linux. Inspect existing services, Docker volumes, ports, memory, nginx and Cloudflare routing in Milestone 7 before proposing changes.

Use an independent Compose stack, private database networking, dedicated persistent storage, multi-stage builds, health checks, restart policies and appropriate resource limits. Production container networking needs an explicit binding override from the local loopback default. Do not copy local host-port exposure into production unquestioningly.

Two-account session authentication and owner/demo row separation are implemented locally; production configuration and access testing remain required before public access. Keep secrets outside Git. No production Docker build, nginx configuration, Cloudflare route, backup job or tested restore procedure exists yet. Local volume persistence is not a backup. Design and test backup restoration before production deployment.

Do not reuse other applications' volumes or modify existing server configurations without approval. Deployment requires explicit authorization.


## Local frontend (Milestone 5)

With Java and PostgreSQL running, use `cd frontend`, `npm ci`, `npm run dev`, then open `http://127.0.0.1:5175/`. Ctrl+C stops this server independently of Java and PostgreSQL. `npm run dev -- --port 5176` changes the UI port; `API_PROXY_TARGET=http://127.0.0.1:18080 npm run dev` changes its API destination. No frontend environment variable needs credentials. Never put secrets in `VITE_*`: those values can enter public browser bundles.

`npm run check` and `npm run build` validate the frontend. `npm run preview` serves the build on loopback:4173 and proxies `/api` locally. The JAR does not contain the frontend. Serving `dist/` alone does not provide an API proxy. Production nginx/API routing, HTTPS, caching and ARM64/AMD64 images remain unimplemented Milestone 7 decisions after NUC inspection and explicit approval.

If the UI loads but requests fail, check Java health on 8080, the configured API proxy target and the Vite terminal. Reloading a browser cannot load new Java code: restart Java after backend changes. Do not repeat a failed POST blindly; inspect saved records first. Port conflicts must be solved by selecting a free port, not stopping unrelated applications. Use a normal browser; VS Code's in-app browser previously prevented form editing.


## Publication readiness — checked 2026-10-22

The local frontend builds and is usable against the Java API. Public hosting is not ready: authentication/account-scoping now exists, but production app images/Compose stack, nginx/API routing and tested production backup/restore procedures are still missing. The local Compose file starts only PostgreSQL; Vite dev/preview are not production hosting. Milestone 6 history/progression also remains unfinished, independently of the deployment blockers.

Before publishing the app, verify production authentication/cookie/proxy configuration, inspect the NUC's services/ports/memory/storage and AMD64 requirements, prepare an isolated production stack, and validate backup restoration. Changes to existing services, Cloudflare routing and deployment need explicit authorization under AGENTS.md. No NUC inspection or infrastructure change was made during this readiness check. The current milestone work is still uncommitted; committing or publishing source is separate from deploying the application.


## Authentication deployment requirements

[AUTHENTICATION.md](AUTHENTICATION.md) documents the implemented local two-account model. The repository has no production password defaults. Provision fresh distinct production BCrypt hashes through private configuration; do not copy test fixtures or commit local credentials. Set `SESSION_COOKIE_SECURE=true`, terminate HTTPS correctly and serve UI/API under one origin. Keep the application loopback/private-network bound; do not expose the Java port or PostgreSQL around the reverse proxy. Review trusted-proxy/forwarded-header handling only after inspecting the real route.

Sessions are process-local and expire after two idle hours; Java restart invalidates every session (useful for password rotation). No remember-me token, SMTP reset or public signup exists. The process-wide 20-login-attempts/minute budget limits work but can temporarily block both accounts; production should add an approved edge limit based on trusted client IP, without disabling the application control or trusting arbitrary forwarded headers. No Cloudflare changes have been made. Database backups and restores must preserve owner keys and be access-controlled; row-scoped app access is not physical database isolation or backup encryption.
