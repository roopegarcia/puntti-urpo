# Owner and demo accounts

## Implemented access model

There are exactly two manually provisioned usernames:

- **owner**: private exercise/workout logging, custom catalogue editing, Swagger/OpenAPI.
- **demo**: read-only invented workouts and shared built-in catalogue. No private owner workouts, custom exercise names or custom notes are visible. Writes are rejected by the backend even when the client has a valid CSRF token.

Both accounts require a password. No public registration, email reset, OAuth, HTTP Basic or JWT login is enabled. The owner password must never be shared. The demo password can be deliberately shared for review after deployment is authorized. Data is not public merely because an account is called demo.

The user requested two accounts in one app. This implementation uses one database with explicit account scoping, rather than the earlier separate deployment/database suggestion. This is a simpler two-account setup with lower process overhead; it does not provide physical database separation. All existing local workouts/custom exercises are assigned to owner by V6. The demo session is entirely invented and versioned in that migration. Neither API payloads nor frontend state can select an arbitrary owner key.

## Local setup

On the implementation Mac, accounts have already been provisioned. `.env` contains BCrypt hashes, not login passwords. The handoff file `.env.accounts` contains the two generated local passwords and is ignored by Git with permissions 0600. Open it locally, save passwords in your password manager, then remove that file. Never paste its contents into issues or source. It is not needed for normal startup after credentials are saved.

For a fresh checkout, create `.env` with database settings as in README, then from `backend/`, with JDK 25 selected:

```sh
./mvnw -q -ntp dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/runtime-classpath.txt
java --class-path "$(cat target/runtime-classpath.txt)" tools/ProvisionAccounts.java
```

The utility refuses to overwrite configured accounts, preserves existing database settings, generates distinct random passwords, writes BCrypt cost-12 hashes to `.env`, and writes the local password handoff with 0600 permissions. It prints only file locations. It is a macOS/Linux local setup utility; it intentionally sets `SESSION_COOKIE_SECURE=false` for loopback HTTP. Do not use these development credentials in production.

From the repository root, re-export `.env`, start PostgreSQL and Java:

```sh
set -a
source .env
set +a
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
export PATH="$JAVA_HOME/bin:$PATH"
docker compose up -d --wait db
cd backend
./mvnw spring-boot:run
```

In another terminal: `cd frontend && npm run dev`, then open http://127.0.0.1:5175/. Select owner or demo and enter its password. Use owner for actual workouts. Both hashes are mandatory and must be distinct valid BCrypt hashes with cost 10–14; there is no fallback password if configuration is missing/invalid. Generated hashes use cost 12.

## Session and CSRF flow

`SecurityConfig` uses Spring Security's form login, not a handwritten password/session controller. `GET /api/auth/csrf` establishes a session and returns a masked token. React sends form-encoded credentials plus that CSRF header to `POST /api/auth/login`; success is 204. Spring changes the session ID on successful login. React obtains a fresh CSRF token and loads `GET /api/auth/me` before rendering private screens.

The session cookie is **PUNTTI_SESSION**, HttpOnly, SameSite=Strict, host-only, path `/`, Secure by default. Local HTTP explicitly sets Secure=false; HTTPS production must use true. Cookies are scoped by host/path, not port. Use the same hostname consistently (127.0.0.1 and localhost are different hosts). Serving UI/API from one production origin avoids a cross-origin credential flow.

Sessions live in the Java process, expire after two idle hours and disappear on Java restart. There is no persistent remember-me token. Logout requires POST plus CSRF, invalidates the session and deletes the cookie. Protected API reads return 401 without authentication; denied roles/CSRF return 403. Passwords and session/CSRF tokens are never stored in localStorage. Spring's default CSRF protection remains enabled, including for login/logout.

If a write encounters an expired session, CSRF may return 403 before authentication can return 401. The client checks current identity, without replaying the write, then asks for login again. Unsaved form state is kept hidden for same-account reauthentication. Switching accounts remounts App and clears private UI state; explicit logout clears it as well. Browser reload still loses unsaved drafts. No response-loss retry is automatic.

Owner Swagger access works after owner login on the same host. For curl/API experiments, use a cookie jar and a CSRF token. The following prompts for the password rather than placing it in shell history; run from a trusted local terminal:

```sh
python3 - <<'PY'
import getpass, http.cookiejar, json, urllib.parse, urllib.request
base = 'http://127.0.0.1:8080'
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def token():
    with client.open(base + '/api/auth/csrf') as r:
        return json.load(r)['token']
csrf = token()
body = urllib.parse.urlencode({'username': 'owner', 'password': getpass.getpass('Owner password: ')}).encode()
with client.open(urllib.request.Request(base + '/api/auth/login', data=body,
        headers={'X-CSRF-TOKEN': csrf, 'Content-Type': 'application/x-www-form-urlencoded'})) as r:
    print('Login status:', r.status)
with client.open(base + '/api/auth/me') as r:
    print(json.load(r))
with client.open(urllib.request.Request(base + '/api/auth/logout', data=b'',
        headers={'X-CSRF-TOKEN': token()})) as r:
    print('Logout status:', r.status)
PY
```

Do not print personal workout responses into public logs. Older curl payload examples in API.md illustrate domain contracts; they now require authenticated cookies and CSRF headers, as above.

## Record isolation

`CurrentAccount.key()` obtains `owner` or `demo` from the authenticated principal. `WorkoutRepository.findByOwnerKey` scopes pages. `findByIdAndOwnerKey` and nested derived queries traverse workout ownership before returning/updating/deleting a record. Response-child queries run only after the parent has passed that check. Foreign IDs return the same 404 as missing IDs. Owner is not an administrator over demo records.

Built-in exercises are shared and read-only. Owner custom exercises are filtered out of demo catalogue search/pagination/direct reads. Only owner can create custom exercises, so the existing global unique-name index remains valid. V6 database checks constrain the supported keys and ensure built-ins have no owner while custom rows belong to owner. PostgreSQL row-level security is not configured: the service/repository boundary enforces privacy. Raw database administrators/backups can access both accounts' records.

## Password rotation and recovery

Generate a new hash interactively from `backend/`:

```sh
java --class-path "$(cat target/runtime-classpath.txt)" tools/HashPassword.java
```

The utility prompts twice without echo and accepts 14+ characters, at most 72 UTF-8 bytes (BCrypt's input limit). It prints only the BCrypt hash. Replace the appropriate quoted `OWNER_PASSWORD_HASH` or `DEMO_PASSWORD_HASH` in private configuration, re-export the environment and restart Java. The restart invalidates all existing sessions, including the old password's sessions. Do not change the username/key, edit V6, or reset the database to change a password. Remove/update the old handoff file so it cannot mislead you. There is no email-based recovery; trusted host/configuration access is the recovery path.

## Verification and production limits

The backend suite runs actual cookie login and CSRF in existing HTTP/PostgreSQL tests. Security-specific tests cover anonymous access, wrong credentials, missing CSRF, session rotation, logout invalidation, role restrictions across all write routes, owner-key spoofing, both directions of direct/nested record isolation, custom catalogue privacy and a bounded login budget. Migration coverage checks legacy ownership and invented demo seed creation without altering stored performance.

The login budget is 20 attempts per minute across the process, counting successes and failures. It is bounded and ignores spoofable forwarding headers, but an attacker can consume the shared budget and temporarily delay both accounts. It resets on process restart; approved production proxy limits and trusted client-IP handling are still needed. The demo password is not an abuse-protection boundary once shared.

The authorized production deployment now uses a separately provisioned empty owner account and invented demo records, with private networking, Secure cookies, nginx same-origin API routing and verified scratch restore. NUC inspection and origin security checks are complete; public TLS/routing status is tracked in DEPLOYMENT.md. Neither account setup nor passing local tests authorizes public hosting or proves the full deployed security chain. See DEPLOYMENT.md for the current authorized NUC/Cloudflare changes; earlier local verification did not authorize those changes by itself.

Primary references used: [Spring form login](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html), [session management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html), [CSRF and SPA integration](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html), [password storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html). Spring Boot manages the compatible Spring Security version.
