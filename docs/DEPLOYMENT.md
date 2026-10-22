# Operation and deployment

## Current deployment status — 2026-10-22

Roope explicitly authorized commit, push and deployment. Application commit `ea7ebb7` and production preparation/fixes are pushed to `origin/master`. The dedicated AMD64 stack is running and verified on the NUC using app image release `62255cd668da`. **Public Cloudflare route creation is awaiting the required final browser-action confirmation; the origin is staged, not yet claimed as publicly live.**

Target hostname: `puntti-urpo.iltapaska.com`. SSH: `roope@192.168.1.21`. Project: `/home/roope/apps/puntti-urpo`. Compose project: `puntti-urpo-production`. The existing `nuc` SSH alias selects `roopeadmin`, which did not authenticate; no SSH configuration was changed.

Roope selected an **empty production owner account**. No local workouts, custom exercises or development credentials were imported. Fresh production credentials are in the private local `.env.production.accounts` handoff; save them in a password manager and remove that file afterward. Production hashes/database password are in a private 0600 `.env` on the NUC. Never share the owner password. Demo receives only the invented V6 session.

## Verified isolation and resources

The NUC is AMD64 Debian, with approximately 7.6 GiB RAM, 4.3 GiB available at inspection and 171 GiB free disk. Existing container/port inventory was inspected before selecting free loopback port 8106. The new stack adds only its own web/backend/db containers, networks and `puntti-urpo-production_postgres_data` volume. Unrelated apps, host nginx, tunnel service and existing volumes were not reconfigured.

Only web publishes `127.0.0.1:8106`. Backend and PostgreSQL have no host ports, using an internal Docker network. Web/backend run non-root with read-only filesystems, dropped capabilities, no-new-privileges, bounded PIDs and temporary writable scratch. Memory caps: backend 768 MiB, db 384 MiB, web 64 MiB, with CPU limits and rotated logs. Measured steady RSS after smoke checks was approximately 290 MiB backend, 44 MiB db and 5 MiB web; this is an observation, not a guarantee of future peak usage. Final containers were healthy with zero restart counts.

Images build natively on the NUC from pinned official multi-architecture Temurin 25, Node 26, nginx and PostgreSQL 18.6 manifests. Runtime Java reports Temurin 25.0.4.1. Health-gated startup verifies PostgreSQL and the Java aggregate health before starting web. nginx serves the built React app and same-origin API. Production session cookies remain Secure/HttpOnly/SameSite=Strict, proxy headers are overwritten, Swagger/OpenAPI/public actuator paths are disabled, and CSP/security headers apply. See [the complete production runbook](../deploy/README.md) and [authentication](AUTHENTICATION.md).

## Actual deployment validation

- Native AMD64 multi-stage builds succeeded. The first fresh wrapper attempt failed safely because missing unzip made it download tar.gz against the ZIP checksum. Apache's published SHA-512 and the pinned ZIP SHA-256 matched; installing unzip in the build stage preserved checksum verification.
- Docker startup exposed two configuration issues (unquoted comma-separated tmpfs values and nginx's additional temporary paths). Quoting the complete mount strings and moving all nginx temporary paths to tmpfs fixed them without weakening read-only root filesystems.
- All three containers reached healthy status; nginx configuration validation passed. Loopback HTML/assets served, private file paths and production Swagger/actuator paths returned 404, anonymous API access returned 401, and CSP/security headers were present on frontend/API responses.
- Trusted-loopback cookie smoke tests verified fresh owner/demo login, Secure cookie attributes, current identity, owner-only writes, CSRF denial, direct/nested private-record 404s and logout invalidation. Owner initially had zero workouts and ten built-ins; demo had one invented workout. A disposable 82.25 kg × 6 at RPE 8.5 returned repetition-based e1RM 98.70. Database restart preserved that set. Only the named disposable workout was deleted afterward; owner is empty again.
- PostgreSQL custom-format backup succeeded and restored into an isolated temporary database with all six migrations and the invented demo session. That scratch database was removed; live records were not overwritten. Backups/configuration are private and ignored by Git.
- A daily backup entry was added without replacing existing user crontab jobs. Schedule is 03:17 America/New_York (10:17 Europe/Helsinki at the verified 2026-10-22 offsets); offsets can differ around seasonal transitions. Seven-day local retention is configured. Off-host encrypted backup storage is **not configured** and same-disk backups do not protect against NUC loss.

## Local development remains separate

The root [README](../README.md) documents JDK 25 selection, ignored `.env`, local PostgreSQL on 54328, Java on 8080 and Vite on 5175. Compose's `.env` is not automatically exported to Java: source it explicitly. Local HTTP uses SESSION_COOKIE_SECURE=false; production forces true. Never copy production passwords into frontend VITE variables or development fixtures.

Local commands from the project root:

```sh
docker compose up -d --wait db
set -a
source .env
set +a
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend
./mvnw spring-boot:run
```

From a second terminal use `cd frontend && npm run dev`. Change Vite's port with `-- --port 5176` or its backend target with API_PROXY_TARGET. Use a normal browser; VS Code's in-app browser previously prevented editing. Backend `/` returning 404 remains expected; the frontend is served by Vite locally and nginx in production.

## Updates, backups and rollback

Use the exact commands in [deploy/README.md](../deploy/README.md). Upload only Git-tracked files, preserve private remote `.env` and backups, tag app images by commit and keep prior image releases. Validate with `config --quiet` (plain config expands secrets), native builds, container health and authenticated origin checks before publication. Never rebuild over the JAR file used by a running local Java process; deployment builds happen remotely and do not touch it.

Before a future schema change, create a backup and verify scratch restore. Code rollback uses the previous known-good image tag; a schema-incompatible rollback needs a separate recovery plan. Never reset a volume, edit applied V1–V6 migrations or restore over live records as a shortcut. Destructive recovery requires an explicit target and authorization. Keep a safe copy of production configuration/password-manager entries separately from database backups.

The first deployment has no previous production release. If rollback is needed before publication, stop only this stack's web/backend and retain its database volume/backups for diagnosis. Existing NUC services must not be stopped to free ports or fix this app.

## Logs and troubleshooting

Use this project's `docker compose --env-file .env -f deploy/compose.yaml ps` and bounded `logs --tail` on the NUC. Secrets can appear in expanded configuration; do not publish config, environment files, raw database dumps or unreviewed logs. Backend responses hide stack traces/internal database details; operational logs may contain diagnostics during a dependency outage.

- Startup failure: check project db health and required credential hashes, not another app's database. Existing-volume database credentials do not change just because `.env` changes.
- 401: sign in; 403 can mean demo write denial or missing/expired CSRF. Reauthenticate without automatically replaying a failed mutation.
- Port conflict: inspect the actual owner and choose a free dedicated port; do not stop unrelated services.
- Public failure with healthy loopback origin: inspect only this hostname's Cloudflare route/DNS/TLS and tunnel health. Do not modify global routes or extract the cloudflared service token.
- Backup failure: inspect this project's private cron log, free disk and archive readability; preserve the last valid backup.
