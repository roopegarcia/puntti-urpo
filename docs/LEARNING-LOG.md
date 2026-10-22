# Learning log

## 2026-10-22 — One-command frontend publication helper

**Request:** simplify publishing copy/UI changes with `./deploy/publish`. Added an executable Bash helper and a separate optional WEB_RELEASE image tag, preserving RELEASE for Java. Clean committed source is required; backend and infrastructure deltas are rejected (apart from the explicit initial WEB_RELEASE fallback). Normal runs perform npm checks/build, stage committed source on the NUC, build native web, recreate only web and verify loopback/public delivery and anonymous API denial. Previous images are retained and failed activation/verification triggers a web rollback attempt. No database writes/migrations, Java restart or Cloudflare changes are part of this helper.

**Actual verification:** Bash syntax and Compose validation passed, including separate backend/web image selection. Nine safety cases passed in temporary repositories with fake SSH/Docker/curl: committed dry run; backend, proxy and Compose rejection; dirty/untracked rejection; successful web-only activation; rollback after public failure; build failure before activation; and concurrent production change refusal. An additional committed temporary-checkout dry run read actual production image tags and passed without production writes. No live publication or rollback was executed. Existing frontend copy edits made concurrently were preserved.

**Files:** new deploy/publish and deploy/test-publish.py; updated deploy/compose.yaml, deploy/README.md, root README/changelog, decisions, Java tutorial section 44, learning log and AGENTS. This helper change is uncommitted and has not been pushed/deployed.

**Learning:** why does Git push not change compiled React assets? Why can WEB_RELEASE change while RELEASE stays the same? Why does a failed frontend build leave the current app serving? Exercise: review the script's dry-run boundary and run it after committing a small copy edit. Understanding has not yet been demonstrated.

---

## 2026-10-22 — Authorized first production publication

**Authorization and scope:** Roope requested commit, push and deployment, chose an empty production owner, and explicitly confirmed the public Cloudflare route. This completes the approved publication; Milestone 6 history/progression remains planned. No local records were imported and unrelated NUC apps/routes/storage were preserved.

**Built:** independent AMD64 Compose project on the NUC, pinned multi-stage Java/React images, non-root read-only app/web containers, private PostgreSQL, resource limits, health-gated startup, nginx same-origin API and security headers. Only nginx binds loopback 8106; existing roopenuc publishes the new hostname over HTTPS. Fresh credentials are in ignored private configuration/handoff files, never Git or frontend assets.

**Actual verification:** backend 73 tests passed (including real PostgreSQL integration); frontend TypeScript/35 tests and production build passed. Native NUC image builds and all three container health checks succeeded. Trusted-loopback checks proved owner/demo login, role/ownership/CSRF boundaries, logout and a real 82.25 kg × 6 set with RPE 8.5/e1RM 98.70; the set survived this project's database restart. Only the disposable verification workout was deleted. Custom-format backup restored successfully into an isolated scratch database with six migrations and the invented demo record; the scratch database was removed. Daily private backups preserve existing cron jobs, with seven-day retention and schedule documented in DEPLOYMENT.md. Off-host backup remains unconfigured.

**Publication:** Cloudflare confirmed route/DNS creation after approval. Public curl checks passed trusted TLS, Finnish HTML/security headers, both real accounts, Secure/HttpOnly cookies, denied anonymous reads/demo writes/missing CSRF, private-record 404s, real saved set estimates and logout. Public verification data was removed; owner is empty and demo has one invented session. Safari visibly rendered the Finnish login, supplied logo and requested footer links. Python urllib received Cloudflare 1010; curl/Safari succeeded without weakening security.

**Problems resolved:** missing unzip caused Maven Wrapper to select tar.gz against its pinned ZIP checksum; verified official archive checksums and installed unzip, preserving validation. Quoted full tmpfs strings to prevent Compose interpreting commas as separate mounts. Moved every nginx temporary directory onto tmpfs to preserve read-only root. No schema migration was edited.

**Files:** new .dockerignore and deploy/ Dockerfiles, HealthCheck.java, nginx.conf, compose.yaml, .env.example, backup, verify-restore and README; .gitignore updated. Updated root README/changelog/AGENTS and architecture, database, authentication, deployment, decisions, Java tutorial and this log. App/auth/footer changes were committed as ea7ebb7; deployment preparation/fixes followed, with running image release 62255cd668da. Documentation-only commits retain that runtime tag. All approved work is pushed to origin/master.

**Learning:** tutorial section 43 explains JDK build/runtime separation and the standalone health JVM using actual source. Understanding has not yet been demonstrated. Three questions: why can the same Java source build on ARM64 and AMD64? How do java -jar and java -cp select their main class? Why must heap allowance leave room for JVM/container overhead? Exercise: trace HealthCheck.java and reproduce a dependency outage only through the disposable local Testcontainers test; do not stop production for practice.

---

## 2026-10-22 — Two-account authentication and private records

**Authorization:** Roope explicitly requested one account for actual workouts and one for demonstration. Implemented locally as a private writable owner and a read-only demo, following the discussed access model. No deployment, push or commit was authorized/performed.

**Built:** Boot-managed Spring Security with exactly two configured BCrypt-backed principals (`owner`, `demo`), default-enabled CSRF, form login/logout, session fixation protection, process-local two-hour sessions, HttpOnly/SameSite=Strict cookies named PUNTTI_SESSION, Secure default with local HTTP override, JSON 401/403 responses, owner-only Swagger and a bounded 20-login-attempts/minute process budget. No signup, HTTP Basic, JWT, remember-me or email recovery. Configuration requires distinct hashes and has no production password fallback.

**Privacy:** V6 assigns every legacy workout/custom exercise to owner, adds owner checks/indexes and seeds one entirely invented demo workout/set. Services/repositories scope lists and direct/nested IDs to the authenticated principal; built-ins are shared, custom owner exercises/notes are hidden from demo. Foreign IDs return 404 and demo writes return 403 even with valid CSRF. Request ownership fields cannot choose a different account. This uses one database with application-enforced scoping, not the earlier suggested separate deployment/database; the infrastructure blast radius is shared and PostgreSQL RLS is not enabled.

**Frontend:** Finnish login/account selection, current-account labels, guarded logout, hidden/disabled demo mutation controls, masked CSRF headers and no stored browser credentials/tokens. Session expiry opens reauthentication without replaying writes; same-account drafts remain mounted but hidden, while switching accounts/remount/logout clears private UI. Login uses the saved/system theme. Responsive login was checked in Safari desktop/320px, including generic wrong-password feedback and cleared password input. No real password was entered through browser automation or printed in tool output.

**Tools/documentation:** added standalone Java source-launcher tools for safe local provisioning and interactive rotation. Generated distinct random local passwords, wrote BCrypt hashes to ignored `.env` and the password handoff to ignored `.env.accounts`, both 0600. Provisioning from a fresh `.env.example` was verified, including blank placeholders, one configuration entry per hash and refusal to overwrite existing accounts. Added AUTHENTICATION.md, updated current README/API/database/architecture/deployment/frontend documentation, decisions, AGENTS, changelog and tutorial sections 40–42. No claim is made that Roope already understands the new concepts.

**Actual verification:**

- `./mvnw -B -ntp verify`: 73 tests across ten report classes, zero failures/errors/skips; 45 unit/parameterized and 28 PostgreSQL/integration. Existing HTTP tests now perform real form login/cookie/CSRF rather than bypassing security.
- New security integration tests verify anonymous read/Swagger denial, health visibility, wrong-password feedback, mandatory CSRF, session rotation, logout invalidation, owner-key spoof prevention, both-direction direct/nested isolation, custom catalogue search/direct-read privacy and role denial across mutation routes. The process-budget unit test verifies the 21st login is blocked while ordinary GETs proceed. Upgrade tests verify legacy owner assignment and one invented demo session.
- `npm run check`: TypeScript clean and 35 tests passed across five files, including login semantic axe audit, CSRF/form encoding/logout, demo controls and session-expiry draft preservation. Production Vite build succeeded. axe/jsdom is bounded evidence, not full accessibility certification.
- Before live migration/restart, created ignored `.env.pre-auth-backup.sql` with 0600 and a private comparison snapshot under ignored backend/target. Restarted only the identified Puntti-Urpo Java process; Compose volume and unrelated services were preserved. Packaged JAR applied V6 and serves the authenticated local API.
- Scripted live cookie login verified both distinct generated passwords, PUNTTI_SESSION/HttpOnly, account identity, owner catalogue/workout responses unchanged against the snapshot, ten built-ins/one invented demo session, demo write denial, missing-CSRF denial, private nested-ID 404s, owner 82.25 kg × 6 at RPE 8.5 returning e1RM 98.70, and logout invalidation. Removed only the explicitly named disposable verification workout afterward; existing personal records were preserved.

**Limits:** public hosting is still unimplemented. NUC inspection, HTTPS/proxy/Secure-cookie validation, production isolation/secrets, complementary trusted-IP login limits and tested backup restore remain required. The local pre-auth dump is a precaution, not a tested production restore solution. Session restart invalidates all logins; the shared process login budget can temporarily delay both accounts under abuse.

**Changed files:** backend pom/config, V6, Exercise/Workout ownership mappings, four repositories/two services; new security classes CurrentAccount, SecurityConfig, SessionController and LoginRateLimitFilter; new AuthenticatedClient, SecurityIntegrationTest and LoginRateLimitTest plus updated migration/foundation/API/service tests; tools ProvisionAccounts/HashPassword; `.env.example`; frontend AuthGate/tests, main/App, api, catalogue/list read-only props, styles and demo/expiry tests; documentation listed above. Local secrets/backups are ignored and not part of the change set.

**Three review questions:** How do authentication, roles and ownership differ? Why is CurrentAccount authoritative over JSON ownership? Why obtain fresh CSRF after login without replaying a failed write?

**Exercise:** use owner and demo in separate browser sessions, create a disposable owner workout and verify its URL is unavailable to demo. Explain the request path from filter chain through scoped repository, then delete only that disposable owner workout. Stop for review; no history/progression or deployment work was started.

---

## 2026-10-22 — Finnish exercise names and publication readiness

**Request:** Roope asked whether the app was ready to publish and requested Finnish exercise names. The prior English-name preference is superseded. Public deployment was not requested or performed.

**Changed:** new V5 in-place rename of the ten built-ins; V1–V4 remain unchanged. IDs, workout references and custom exercises are preserved. Updated catalogue API tests, foundation migration count and the upgrade test; added a PostgreSQL collision-rollback test. Updated frontend fixtures, README catalogue/search, API examples, architecture/database/deployment docs, decisions, changelog, AGENTS and tutorial section 39. The Java API returns the current Finnish catalogue names in both catalogue and nested workout reads.

**Actual verification:** `./mvnw -B -ntp verify` succeeded: 68 cases across eight reports, zero failures/errors/skips (44 unit/parameterized, 24 PostgreSQL/integration). The first run revealed duplicate-name assertions still using the old English spelling; corrected them to Finnish and reran successfully. Upgrade coverage verifies stable IDs/references, saved kg/reps/RPE/workout title, unchanged custom data, all ten names and repeat startup. The separate collision schema confirms every catalogue row remains unchanged when a custom Finnish name blocks V5. `npm run check` passed all 30 frontend tests; production build succeeded. A temporary packaged JAR on a random loopback port applied V5 to the local database and returned healthy status; reads through the existing backend confirmed the ten names and unchanged IDs/custom rows/workout details. Only the temporary process was stopped. `git diff --check` passed.

**Readiness:** local functionality is verified, but public hosting remains blocked by missing authentication/data separation, production packaging/routing and tested backup restoration. NUC inspection (services, ports, memory, AMD64/storage) and explicit deployment approval remain required. History/progression is still Milestone 6. No remote push, NUC or Cloudflare change was made; current changes remain uncommitted.

**Learning check:** Why is V5 added instead of editing V3? Why do workout references survive a rename? What causes the collision test to roll back the whole change? Exercise: read tutorial section 39 and trace one catalogue ID through the upgrade test without changing the everyday database.


## 2026-10-22 — Author footer

Roope requested a footer matching the supplied reference, with his four exact destinations. Added `components/Footer.tsx` and replaced the former logo/tagline footer in `App.tsx`. A serif author heading, accurate Java/React description, labelled link navigation with accent arrows, separator and dynamic copyright row follow the reference layout; responsive wrapping and semantic theme tokens support narrow screens and both themes. No Java, API, database, or deployment behavior changed. Frontend documentation and changelog updated. Verification: all 30 frontend tests passed, production build succeeded, and Safari desktop/320px checks confirmed readable layout and all four exact link destinations. Changes remain uncommitted.


## 2026-10-22 — Supplied logo applied

Roope supplied a PNG logo and requested replacing all site logos. Preserved the original image/color profile in `frontend/public/logo.png` for header/footer branding. Added a matching vector symbol for the hero, favicon, empty states and exercise/workout icons; the symbol uses the supplied P3 red converted to sRGB (#b9341d). The full logo retains a white surface so its dark wordmark stays readable in both themes. Updated `App.tsx`, `WorkoutList.tsx`, `ExerciseLibrary.tsx`, `Shared.tsx`, `styles.css`, favicon and frontend documentation/changelog. Verified light/dark desktop rendering in Safari; all 30 frontend tests and the production build pass. No data/API/schema changes, commit or deployment.


## 2026-10-22 — Milestone 5 complete: Finnish React frontend

**Authorization and commit:** Roope approved committing Milestone 4 and starting the next milestone. Milestone 4 is local commit `4ac57f9` (`Complete milestone 4 training estimates and precision tests`). The frontend milestone is implemented and left uncommitted for review. Nothing was pushed or deployed.

**Built:** a separate exact-pinned React/TypeScript/Vite project; responsive exercise catalogue/search/custom create/edit; paged workout navigation and creation; workout/exercise notes and metadata; draft/completed sets, Finnish decimal entry, optional RPE, append convenience and finish timing; explicit deletion confirmations; server-derived RIR/repetition-based e1RM with visible reasons/warnings; light/dark themes and original SVG identity. Existing APIs, Java production code and migrations are unchanged. Basic listing supports opening the editor; history analysis, comparisons/charts/statistics remain Milestone 6.

**Reliability:** explicit save ownership, no automatic mutation retries, stale GET cancellation, unsaved/pending navigation guards, timestamp precision preservation and merges that retain other row drafts. The saved logged exercise remains visible if creating its first set fails. Real Safari testing exposed an incorrect discard prompt after creation and first-set focus stealing focus after later appends; both were corrected and rechecked, with a focus regression test added. Mobile checking found a hidden-text icon control missing its accessible name and narrow numeric input clipping; explicit labels and two-column small-screen fields fixed these.

**Java concepts reinforced:** JSON DTOs versus entities and TypeScript interfaces, runtime validation versus compile-time shape checking, BigDecimal authority at a JavaScript transport boundary, request-scoped transaction boundaries, mapper-derived response values, and separate npm/browser and Maven/JVM builds. Tutorial sections 36–38 trace the actual frontend-to-Java save flow. No understanding or confusion is inferred from milestone approval; Roope has not yet demonstrated these concepts.

**Actual verification:**

- `npm run check`: TypeScript clean; 30 tests across four files passed, zero failures. Cases include locale/ranges, zero load/null drafts, date/payload preservation, API 204/errors/no retry, set completion/estimates/warnings, failed-input retention, deletion cancellation, unsaved navigation, post-create navigation, metadata precision/unrelated drafts, duplicate-name preservation, partial creation recovery and appended-set focus.
- axe-core: zero semantic violations on the empty dashboard and populated editor in jsdom. Color contrast is disabled in that non-rendering environment. Manual token contrast calculations: checked text pairs >=5.52:1; input-border contrast 3.65:1 light and 4.78:1 dark. This is bounded evidence, not a full WCAG certificate; physical mobile and screen-reader testing remain follow-up evaluation.
- `npm run build`: successful TypeScript and Vite production build, 24 modules; JS 255.35 kB (79.21 kB gzip), CSS 22.46 kB (5.28 kB gzip). The separate built preview served HTTP 200 and proxied a real saved workout from Java/PostgreSQL; Safari opened the packaged UI successfully.
- Safari desktop: create workout, choose Bench Press, enter comma decimal 82.25 kg × 6 at RPE 8.5, complete set, see RIR 1.5 and e1RM 98.70 with SUBMAXIMAL_RPE_NOT_ADJUSTED. Added drafts copied load/reps, cleared RPE/completion and focused the new load field. Keyboard Tab moved from reps to RPE. Deletion confirmation was cancelled and returned focus to its trigger. Both themes rendered.
- Safari responsive mode: editor inspected at 390px and 320px, library at 320px. After the mobile grid correction the full load remained readable without horizontal clipping. Desktop Brave dashboard was also inspected before moving QA to a separate Safari window while Roope tested Brave.
- A scripted read through preview confirmed stored numbers and estimate warnings. Only explicitly named disposable workouts IDs 4 and 5 were removed afterward, with names checked before deletion and subsequent 404 verified. Catalogue/custom data was identical before/after cleanup; the database volume and user's running backend were preserved.
- The local database password was checked for absence from frontend source/lockfile/build without printing it. No frontend credentials were introduced. `git diff --check` passed. An optional npm audit attempt stalled and was cancelled; no completed vulnerability-audit result is claimed.
- Backend tests were not rerun for this frontend-only milestone; the unchanged Java baseline is the 67 passing tests documented under Milestone 4. Frontend mocked HTTP tests do not replace PostgreSQL integration coverage.

**Changed files:** `.gitignore`, `AGENTS.md`, `README.md`, `CHANGELOG.md`; `docs/API.md`, `ARCHITECTURE.md`, `CALCULATIONS.md`, `DATABASE.md`, `DECISIONS.md`, `DEPLOYMENT.md`, `JAVA-FOR-ROOPE.md`, `LEARNING-LOG.md`; new `docs/FRONTEND.md`. New frontend files: `package.json`, `package-lock.json`, `tsconfig.json`, `vite.config.ts`, `index.html`, `public/favicon.svg`; `src/main.tsx`, `App.tsx`, `api.ts`, `types.ts`, `utils.ts`, `hooks.ts`, `styles.css`; components `Shared.tsx`, `WorkoutList.tsx`, `ExerciseLibrary.tsx`, `WorkoutEditor.tsx`, `SetRow.tsx`; tests `App.test.tsx`, `api.test.ts`, `utils.test.ts`, `components/SetRow.test.tsx`, `test/fixtures.ts`, `test/setup.ts`. Generated node_modules/dist are ignored.

**Three review questions:** Why must Java validate a TypeScript client request? Where are estimates calculated and rounded? Why can the first of two HTTP writes remain committed if the second fails?

**Exercise:** follow the Milestone 5 exercise in JAVA-FOR-ROOPE: create a disposable workout, compare a completed six-rep set and an eleven-rep set, inspect the response, reload and explain the persistence and estimate differences from the Java source.

**Review state:** Milestone 5 implemented and locally verified. Stop before Milestone 6 until explicitly approved. Authentication, NUC inspection and deployment remain unimplemented; no changes to other services or Cloudflare routing.

---

## 2026-10-22 — Milestone 4 complete: RPE/RIR and repetition-based e1RM

**Authorization and commit:** Roope explicitly approved committing the previous milestones and proceeding. Milestones 2–3 are now local commit `5cd7805` (`Complete exercise catalogue and workout logging milestones`), following Milestone 1 commit `34a5e14`. Milestone 4 changes remain uncommitted for review. Nothing was pushed or deployed.

**Built:** pure BigDecimal domain calculations returning an immutable result with explicit availability/status/warning codes. Completed-set RIR uses the conventional subjective 10 − RPE approximation; eligible completed working sets use repetition-based Epley with a recorded-load single-rep convention. One response-mapper path supplies additive read-only estimates on creation, replacement, single and nested reads. No migration, backfill, calculation endpoint, cache or new dependency was needed.

**Policy and limits:** e1RM requires positive load and 1–10 repetitions on completed WORKING sets. Missing/ineligible values are null with a reason rather than zero. RIR is independently available for completed sets with recorded RPE. Submaximal/unknown effort and unverified singles have visible warning codes. RPE does not change the Epley value. Final-only kg rounding is two decimals, HALF_UP. Arithmetic eligibility is not exercise/person validation or a measured record. CALCULATIONS.md documents primary research sources, applicability and a separate RPE-aware candidate as an unimplemented inference, not a validated model.

**Java concepts added:** pure functions, scalar/domain boundaries, exact multiplication/division with explicit rounding, Optional versus nullable serialized fields, immutable record collection copying, enum statuses/warnings, and JUnit parameterized input sources. Tutorial chapters 31–35 cite actual code.

**What confused Roope:** not yet discussed; no Java understanding is assumed or claimed. The working milestone is complete before tutorial review, following the agreed learning workflow.

**Actual verification:**

- `./mvnw -B -ntp verify`: BUILD SUCCESS; 67 tests/cases, zero failures/errors/skips, confirmed from eight XML reports. 44 unit/parameterized and 23 PostgreSQL/integration cases.
- 36 pure calculation cases covered all nine RPE steps, missing/invalid effort, single/ten/eleven-rep boundaries, nullable/zero/negative load and missing reps, large input values, final rounding and half-way behavior. Submaximal and maximal-reported RPE produce the same Epley value but different RIR/warnings.
- Two added HTTP/PostgreSQL tests checked POST/PUT/GET and nested-read agreement, updated/draft availability, warmup exclusion, unknown-effort/single warnings and zero-load handling.
- Packaged JAR started against the existing database, returned RIR 1.5 and repetition-based e1RM 98.70 kg for 82.25 kg × 6 at RPE 8.5, with SUBMAXIMAL_RPE_NOT_ADJUSTED. Nested detail matched the standalone response.
- Editing to 11 reps produced null e1RM with UNSUPPORTED_REPETITIONS while retaining estimated RIR 0 at RPE 10. OpenAPI exposed the new result fields and warning codes.
- Fingerprints of every pre-existing row in all four domain tables matched after removing only the disposable verification workout. Flyway history remained four applied migrations. No observations, custom exercises or schema were rewritten.
- Only the independently launched verification JAR was stopped; the user's existing database/application were left running. Restart the user-run application to load the new response fields.
- `git diff --check`, authored whitespace, local Markdown links, XML report and credential-exclusion checks passed. No local password appeared in Git-visible files or validation logs.

**Added files:**

- `backend/src/main/java/com/punttiurpo/domain/SetEstimates.java`
- `backend/src/main/java/com/punttiurpo/domain/TrainingCalculations.java`
- `backend/src/test/java/com/punttiurpo/domain/TrainingCalculationsTest.java`
- `docs/CALCULATIONS.md`

**Updated files:**

- `AGENTS.md`
- `CHANGELOG.md`
- `README.md`
- `backend/src/main/java/com/punttiurpo/dto/WorkoutSetRequest.java`
- `backend/src/main/java/com/punttiurpo/dto/WorkoutSetResponse.java`
- `backend/src/main/java/com/punttiurpo/mapper/WorkoutMapper.java`
- `backend/src/test/java/com/punttiurpo/WorkoutApiIntegrationTest.java`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/DATABASE.md`
- `docs/DECISIONS.md`
- `docs/JAVA-FOR-ROOPE.md`
- `docs/LEARNING-LOG.md`

**What Roope should be able to explain after studying:** why calculations are separate from persistence, why rounding is final-only, how null/zero/unavailable differ, why subjective RPE does not silently turn a repetition estimate into a measured maximum, and what the unit/integration tests do and do not prove.

**Three questions for later:**

1. Why multiply before dividing and round only the final kg result?
2. How do missing RIR, zero RIR and unavailable e1RM differ?
3. Why does RPE change warnings/RIR but not the implemented Epley estimate?

**Suggested exercise:** compare a disposable completed 100 kg × 6 set at RPE 8.5 and 10, an 11-rep set and a draft. Explain the numeric values/status/warnings from the pure function, then remove only that disposable workout.

**Review state:** Milestone 4 implemented and verified. Stop before Milestone 5 (React frontend) until approved. No RPE-adjusted estimate was implemented.

---

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
