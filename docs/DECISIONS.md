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

## 2026-10-22 — Derived set estimates without schema changes

**Context:** Milestone 4 must calculate RIR/e1RM while keeping recorded performance distinct from predictions and persistence code.

**Options:** persist/update estimates in every write path, add a calculation endpoint, or derive a nested response value through one mapper path.

**Choice:** pure `domain/TrainingCalculations` scalar functions return immutable `domain/SetEstimates`; WorkoutMapper adds that read-only result to all set responses. No entities, repositories, Spring or external services enter the calculator. No migration, cache, endpoint, backfill or new dependency is added.

**Reasoning:** a single response-mapping path keeps creation, updates, single reads and nested reads consistent. Existing sets gain current derived values without rewriting user data.

**Trade-offs:** API responses gain an additive object. Derived values are computed on read, not stored historical predictions; a future formula revision must preserve clear method semantics rather than silently treating old observations as measured records. Structured status/warning codes need Finnish rendering in the frontend milestone.

## 2026-10-22 — Approximate RIR and explicitly unadjusted Epley

**Context:** RPE expresses subjective effort; a completed set below maximal effort must not silently be treated as an objectively maximal attempt.

**Options:** assume missing RPE means maximal effort, adjust repetitions using inferred RIR immediately, or retain an explicitly repetition-based estimate with effort warnings.

**Choice:** completed-set RIR is the conventional `10 − RPE` approximation on the supported half-step 6–10 scale; missing effort/drafts yield null. e1RM uses the approved Epley formula from kg and completed reps only. A recorded RPE below 10 adds a submaximal-not-adjusted warning; absent RPE adds an unknown-effort warning. One repetition returns its recorded load with an always-present not-verified-maximum warning. No RPE-adjusted estimate is implemented.

**Reasoning:** preserves the limited meaning of the observed set and makes the distinction visible in API fields and Swagger descriptions. The single-rep rule is an explicit product convention, not proof that any recorded single was maximal.

**Trade-offs:** submaximal sets still have a numerical repetition-based value when otherwise eligible, but it must not be presented as a measured or RPE-adjusted maximum. RIR remains subjective even when the arithmetic is exact. CALCULATIONS.md documents sources, limitations and a separate RPE-aware candidate as an unvalidated inference requiring further validation before any implementation.

## 2026-10-22 — Eligibility, reasons and final-only decimal rounding

**Context:** zero-load, draft, warmup and high-repetition sets may be valid logs but unsuitable for the initial estimate policy.

**Options:** emit a number for every log, use zero to mean unavailable, or return null with explicit eligibility reasons.

**Choice:** e1RM requires completed WORKING sets, positive load and 1–10 repetitions. Exclusion precedence is NOT_COMPLETED → NOT_WORKING_SET → NO_POSITIVE_LOAD → UNSUPPORTED_REPETITIONS. Return null with the first reason; allow completed-set RIR independently. Use BigDecimal `weight × (30 + reps) / 30`, rounding only final kg to two decimal places with HALF_UP. Singles use the recorded load.

**Reasoning:** prevents missing/unfit values from looking like zero strength, bounds the initial repetition range and avoids premature division rounding. AVAILABLE means policy eligibility, not universal exercise validity. The cutoff/warmup rule is a conservative product choice; no blanket exercise-specific scientific validation is claimed.

**Trade-offs:** sets over 10 reps still store normally but do not contribute an e1RM through this policy. Machine/cable and dumbbell conventions remain exercise/equipment dependent. Two-decimal output precision does not represent prediction accuracy. Pure parameterized tests cover all supported RPE steps, numerical/eligibility boundaries and rounding; HTTP/database tests verify consistent serialization and recalculation.

## 2026-10-22 — Small React client with a separate local proxy

**Context:** Milestone 5 needs usable gym logging over the already tested Java API, without beginning history analytics or deployment.

**Options:** add routing/state/component frameworks, embed a bundle in the JAR now, or use conventional React state and a separate Vite development server.

**Choice:** React 19.3.1, TypeScript 7.0.2 and Vite 8.3.5, exact npm pins and lockfile; Node >=24. Hash routes and local component state, semantic custom CSS and original SVGs. Loopback:5175 proxies `/api` to Java; preview uses loopback:4173. No backend CORS or packaging change.

**Reasoning:** keeps build/runtime boundaries visible for learning, uses the existing API unchanged and avoids dependencies for a three-route interface. An explicit port avoids the unrelated local project using 5173. Both themes use readable numeric inputs and a two-column set form on narrow screens.

**Trade-offs:** two local processes plus PostgreSQL; npm and Maven have separate dependency graphs. Hash routing is deliberately simple. There is no production frontend host/API routing, auth, charting or history aggregation yet. Direct package versions and installed lockfile are pinned; future upgrades require fresh compatibility verification.

## 2026-10-22 — Explicit saves and server-derived estimates

**Context:** silent retries or form resets can corrupt a workout user's intent, and Java must remain the calculation authority.

**Options:** autosave/offline persistence, a global cache, or explicit resource saves with local draft ownership.

**Choice:** each form owns a draft and pending state; API responses merge only their relevant subtree. Warn on unsaved navigation, block navigation during writes, cancel obsolete GETs and bound requests to 15 seconds. Never automatically retry mutations. A successful create/delete explicitly permits its destination after the response. Keep a partially created logged exercise visible when its first-set request fails. Browser storage retains only theme preference. Display backend estimates/reasons/warnings in Finnish without strength math in TypeScript.

**Reasoning:** makes persistence reviewable and preserves unrelated edits. A lost response cannot prove failure; explicit recovery avoids duplicate appends. Navigation authorization after saves avoids a real Safari race with React effect cleanup. A one-time focus marker prevents first-set focus from stealing focus after later appends.

**Trade-offs:** more explicit save actions; no offline support, idempotency keys, optimistic locking or multi-tab merge. First exercise/set creation is two API calls with recoverable partial success. Client number parsing is only transport preparation; Java BigDecimal and database constraints own exact storage/calculation. Automated semantic audits plus manual responsive/keyboard checks provide bounded evidence, not full accessibility certification.


## 2026-10-22 — Translate the existing catalogue, preserving identity

**Context:** Roope replaced the earlier English-name preference with Finnish exercise names.

**Options:** edit an applied seed migration, translate only in React, replace rows, or add a versioned in-place rename.

**Choice:** V5 updates only known built-in names to Finnish, preserving IDs and references. Existing custom names/fields are untouched. Name collisions fail atomically rather than modifying user data.

**Reasoning:** makes search, Swagger, nested workout reads and UI agree while keeping migration history valid and existing workouts linked to the same exercises.

**Trade-offs:** old workouts display the catalogue's current name, because names are not historical snapshots. A custom-name conflict needs deliberate resolution before startup can finish; integration tests verify safe rollback. Translations use conventional Finnish names without changing exercise scope or metadata.


## 2026-10-22 — Two manually configured session accounts with scoped records

**Context:** Roope authorized one account for actual workouts and one for demonstration after discussing read-only demo access. Deployment remains a separate approval.

**Options:** separate app/database deployments, multi-user registration, JWT/OAuth, or two fixed principals with sessions and explicit record ownership.

**Choice:** Boot-managed Spring Security; fixed owner/demo usernames with distinct externally provided BCrypt hashes; standard form login/server sessions and default CSRF. Owner may write; demo is read-only. V6 assigns legacy records to owner and creates invented demo data. Account-scoped repositories protect workout and nested IDs; catalogue built-ins are shared and owner custom exercises remain private. No request field chooses ownership. React authenticates before mounting App, clears state on logout/account switch and preserves same-account drafts behind reauthentication without write retries.

**Reasoning:** matches the two-account request and is understandable as a conventional Spring filter/service/repository boundary. No signup, mail service, token refresh protocol or additional identity provider is needed. No plaintext password or hash is committed; local setup generates a private handoff file and ignored environment hashes. Session cookies default Secure/HttpOnly/SameSite=Strict, with an explicit local HTTP override.

**Trade-offs:** one application/database shares infrastructure and service blast radius; this is weaker physical separation than the earlier separate-stack suggestion. Fixed keys/check constraints intentionally do not support arbitrary future users. Supporting additional writable users requires a deliberate account/domain migration and per-user catalogue uniqueness policy. Sessions are process-local and invalidate on restart; password recovery uses trusted host configuration. The process-wide login budget is bounded but can temporarily delay both users and needs complementary production proxy controls. Production HTTPS/routing/backups/NUC review remain separate.

## 2026-10-22 — Native AMD64 build and isolated production origin

**Context:** Roope authorized commit/push/deployment on a shared Intel NUC after local owner/demo verification. Inspection found approximately 4.3 GiB available RAM, 171 GiB free disk and loopback 8106 unused. The user selected an empty production owner account.

**Options:** build Intel images under emulation on the ARM64 Mac, copy a local JAR, or build pinned multi-stage images natively on the NUC; reuse existing infrastructure or create a dedicated Compose stack.

**Choice:** native AMD64 build from Git-tracked source using pinned official Temurin/Node/nginx/PostgreSQL manifests. Dedicated puntti-urpo-production project, loopback nginx:8106, private backend/db network and independent volume, explicit resource/privilege/log limits. Fresh production credentials are privately provisioned; no local data import. Existing cloudflared handles only a newly approved hostname route; no unrelated services/configuration are changed.

**Reasoning:** native builds avoid cross-architecture emulation and keep the runtime reproducible. The new app adds approximately 340 MiB measured steady memory within configured caps. Same-origin API routing supports secure sessions and requires no browser CORS widening. The backend image's build stage explicitly installs unzip so the script-only wrapper selects its checksum-pinned ZIP; Apache SHA-512 and pinned SHA-256 were independently verified after discovering automatic tarball fallback in the minimal image.

**Trade-offs:** Maven/npm dependencies still download during the first native build; image pinning requires intentional security updates. The shared NUC remains a shared host failure domain. Runtime images omit build tools and tests; local test suites plus real origin smoke checks provide separate evidence. The first deployment has no prior release for code rollback. Secure cookies require public HTTPS even though the tunnel origin is loopback HTTP.

## 2026-10-22 — Private backup archive and non-destructive restore proof

**Context:** volume persistence alone does not prove recoverability; other NUC applications must not be touched.

**Options:** rely on the volume, overwrite the live database during a restore drill, or restore a custom-format archive into a dedicated scratch database.

**Choice:** private pg_dump custom archives, seven-day local retention and a daily job preserving existing crontab entries. Verify restore through a uniquely named temporary database in only this app's PostgreSQL container; install cleanup only after successful scratch creation and drop only that scratch database. Separate secrets remain in private configuration/password-manager storage.

**Reasoning:** proves archive readability and restoration without replacing production records or allocating another persistent stack. Initial verification restored all six migrations and the demo session. A restart-persistence test used and removed only a named disposable owner workout.

**Trade-offs:** local backups do not survive host/disk loss. An encrypted off-host destination/schedule is not configured automatically. The restore drill proves database restoration, not full disaster recovery or arbitrary future migration rollback. Destructive recovery requires explicit target approval.


## 2026-10-22 — Explicit frontend publication command

**Context:** Roope wants a short release path for copy edits. React copy is compiled into static assets; Git push alone cannot change nginx's existing image.

**Decision:** add a readable Bash publish helper using the existing SSH/Git archive and native NUC build. Keep a separate optional WEB_RELEASE while RELEASE identifies Java. Reject backend/infrastructure changes, run frontend checks, recreate only web and retain/restore the previous image after a failed verification. Full releases retain their existing manual backup/migration workflow and clear the frontend override before building.

**Trade-offs:** brief web downtime, local Node/npm and SSH/Git history are required, and public checks verify reachability/security boundaries rather than every authenticated flow or copy detail. There is no automatic GitHub deployment and no general deployment platform.
