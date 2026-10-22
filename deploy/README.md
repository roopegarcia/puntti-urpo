# Production stack

Deployment was explicitly authorized on 2026-10-22. Target: `puntti-urpo.iltapaska.com` through the existing Cloudflare Tunnel `roopenuc`, to the dedicated nginx origin `127.0.0.1:8106` on the Debian AMD64 NUC. Use `ssh roope@192.168.1.21`; the local `nuc` alias points at another account and does not currently authenticate.

## Isolation and runtime

This Compose project is **puntti-urpo-production**, independent of the local Compose project and all other NUC apps. Only nginx publishes a loopback port. Backend/PostgreSQL have no published ports and communicate on the internal private network; PostgreSQL owns only this project's `postgres_data` volume. App/web run as non-root with read-only filesystems, temporary writable scratch space, dropped capabilities, bounded PIDs and memory/CPU limits. Docker logs rotate. Images pin multi-architecture official base-image manifest digests; build on the NUC for native AMD64 rather than emulating Intel on the ARM64 Mac.

The JDK build stage installs unzip so Maven Wrapper downloads the checksum-pinned ZIP rather than switching to a differently checksummed tarball. This preserves distribution verification; no checksum is disabled.

The backend's combined app/database health gates web startup. The web health endpoint checks nginx separately. The JVM heap is bounded relative to the 768 MiB container, with limits on metaspace/code cache/thread count; PostgreSQL has 384 MiB and 64 MiB shared buffers; web has 64 MiB. Inspect actual RSS/OOM/restart behavior after deployment, rather than treating configured limits as measurements.

nginx serves the built React bundle and proxies same-origin `/api` without path rewriting or automatic upstream retries. Incoming forwarded scheme/host headers are replaced; Spring trusts only this private proxy path. The origin is intentionally HTTP on loopback behind Cloudflare HTTPS, while session cookies remain Secure/HttpOnly/SameSite=Strict. Production Swagger/OpenAPI and public actuator paths are disabled/blocked. The aggregate Java health remains available inside its container. CSP/security headers apply to public frontend and API responses. Client and Java login budgets are process-wide protective limits; nginx currently sees the tunnel/bridge rather than distinct public users. No arbitrary CF-Connecting-IP header is trusted. Per-client edge policy can be reviewed separately without weakening these controls.

## Secrets and first database

The user chose **an empty owner account**; do not import local workouts, custom exercises, local database credentials or personal backups. Flyway creates the ten Finnish built-ins plus the invented read-only demo session. Production account passwords are freshly generated and differ from local development. Production `.env` belongs only in the private remote project directory, permissions 0600. Keep the account password handoff in a password manager, not Git.

Required private configuration: `RELEASE`, `WEB_PORT=8106`, `POSTGRES_PASSWORD`, `OWNER_PASSWORD_HASH` and `DEMO_PASSWORD_HASH`. Compose uses explicit required values and never prints secrets during validation (`config --quiet`). Its backend forces Secure cookies. Never run plain `docker compose config` in shared output, since it expands secret environment values.

## Upload and start a committed release

Run frontend/backend checks before releasing. Do not build over a running local JAR file; stop/restart that local process deliberately if rebuilding its artifact. The deployment build runs on the NUC and does not touch local target files.

From a clean, committed checkout on the Mac, upload only Git-tracked files:

```sh
release=$(git rev-parse --short=12 HEAD)
git archive HEAD | ssh roope@192.168.1.21 'cd ~/apps/puntti-urpo && tar -xf -'
```

Provision the production `.env` securely beforehand; never include it in the Git archive. On the NUC:

```sh
cd ~/apps/puntti-urpo
# Set RELEASE in private .env to the uploaded commit's identifier.
docker compose --env-file .env -f deploy/compose.yaml config --quiet
docker compose --env-file .env -f deploy/compose.yaml build --pull
docker compose --env-file .env -f deploy/compose.yaml up -d --wait --wait-timeout 180
docker compose --env-file .env -f deploy/compose.yaml ps
curl -fsS http://127.0.0.1:8106/healthz
```

Verify the loopback origin first: static HTML/assets, anonymous API denial, Secure cookie attributes, CSRF login/logout, owner/demo isolation and actual saved-set behavior. Secure cookies must be sent manually for trusted loopback HTTP origin smoke tests; browsers correctly require public HTTPS. Do not turn off Secure in production to make a loopback test pass. Create the Cloudflare published route only after origin checks and restore verification pass. Other routes/services must remain unchanged.

The existing Cloudflare tunnel is remotely managed. No root token or unrelated nginx config needs changing. Add only `puntti-urpo.iltapaska.com` → `http://127.0.0.1:8106`, preserve all existing routes, then check public HTTPS and account behavior. Creating a route is separate from staging a healthy origin; record actual publication status in DEPLOYMENT.md.

## Backups and tested restore

On the NUC from this project's directory:

```sh
./deploy/backup
./deploy/verify-restore backups/puntti-urpo-TIMESTAMP.dump
```

Backups are PostgreSQL custom-format archives, private mode 0600, retained for seven days in the project's private `backups/` directory. The script checks archive readability and uses only this Compose project's db. The restore verification creates an isolated `punttiurpo_restorecheck_TIMESTAMP` database within this dedicated PostgreSQL container, restores the archive with `--exit-on-error`, checks schema/record scopes and drops only that scratch database afterward. It never overwrites the live application database. A successful scratch restore proves the database archive can be read/restored; it is not off-host disaster recovery.

Schedule only this app's backup, preserving existing user crontab jobs:

```cron
17 3 * * * cd /home/roope/apps/puntti-urpo && ./deploy/backup >> backups/cron.log 2>&1
```

The NUC's timezone determines the daily schedule. Review `backups/cron.log`, disk usage and failed backup exits. Keep an encrypted off-host copy and a separate safe copy of production secrets; same-disk backups do not survive loss of the NUC. Off-host storage has not been configured automatically.

For disaster recovery, stop web/backend, preserve the damaged volume, initialize a **new dedicated volume**, recreate the database role using private configuration, restore the selected archive with `pg_restore --no-owner --exit-on-error`, then start the compatible app image and validate account scoping/health. Do not use `down -v` or restore over live user data as a shortcut. A destructive restore requires explicit authorization and a concrete recovery target.

## Rollback and safe updates

Images are tagged by committed release ID. Keep the previous backend/web images and record each deployment. Before future migrations take a backup and test restore. To roll back code, set `RELEASE` to the prior image tag and use `up -d --no-build --wait`; do not pull a floating tag. If a schema change is incompatible, code rollback alone is insufficient: stop and plan a data-preserving recovery. Never edit applied migrations or discard the database volume.

The first deployment has no older app release; rollback before publication means stop only this stack's web/backend and keep the new volume/backups for diagnosis. Do not restart unrelated containers, host nginx or cloudflared to resolve an application failure.
