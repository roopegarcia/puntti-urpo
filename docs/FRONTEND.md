# React frontend — Milestone 5

## Run and verify

Start PostgreSQL and Java using [README](../README.md), then in a second terminal:

```sh
cd frontend
npm ci
npm run dev
```

Open **http://127.0.0.1:5175/** in a normal browser and sign in with the local credentials from `.env.accounts`. Node >=24 is required; verification used Node 26.9.0 and npm 12.2.0 on ARM64 macOS. Exact direct dependency versions are in `package.json`; commit `package-lock.json` with intentional upgrades and use `npm ci` for reproducible installation. No global React/Vite installation is needed. Maven does not install npm packages.

The loopback-only dev server uses strict port 5175; another project already occupied 5173 during implementation. Change with `npm run dev -- --port 5176`. Its `/api` proxy targets `http://127.0.0.1:8080`, configurable by `API_PROXY_TARGET`. This is a server-side development variable, not a browser secret. No root `.env` credentials are needed by React. Swagger stays at `http://127.0.0.1:8080/swagger-ui/index.html`; backend `/` is intentionally 404.

```sh
npm run check
npm run build
npm run preview
```

`check` performs TypeScript checking plus 35 Vitest tests. Build output is ignored `dist/`. Preview serves that output at loopback:4173 with the same local API proxy; Java must still run. Ctrl+C stops either frontend process. Neither Vite server is a production deployment.

## Everyday workflow

1. Select **Aloita harjoitus**, optionally enter a title and choose **Aloita nyt**. The local calendar date is the training date and the start time is recorded as an instant.
2. Select **Lisää liike** and a catalogue exercise. A saved logged exercise and first draft set are created. Built-in names now use the ten Finnish translations from V5; instructions remain Finnish. Existing custom exercises are preserved.
3. Enter load and repetitions; comma or dot decimals are accepted, with at most two kg decimals. RPE is optional, 6–10 in 0.5 steps. **Valmiiksi** saves the current values and marks completion; a completed set requires a load (zero is allowed) and positive reps. **Tallenna muutokset** saves without changing completion.
4. **Lisää sarja** copies load, reps and type from the latest completed set in this exercise, falling back to its latest draft. RPE and completion are cleared. Focus moves to the new load field. This is a convenience, not a load recommendation.
5. Expand workout or exercise notes to edit them, then explicitly save. The workout date and optional start/end times are editable. Unchanged timestamps retain seconds/milliseconds when other metadata is saved. Times display in the browser's local timezone.
6. **Päätä harjoitus** records the end time; it does not mark draft sets complete or lock future editing. It is disabled while forms have unsaved changes or pending writes. Workouts without a start time show **Aloita ajanotto** first. Existing sessions can be opened from the basic paged list.
7. Under **Liikkeet**, submit a name search or choose **Oma liike**. Custom exercises are editable; built-ins are read-only. The backend enforces name uniqueness. No catalogue deletion is exposed.

Set, logged-exercise and whole-workout deletion each require an explicit inline confirmation; Cancel returns focus to the trigger. Deletion has no undo. Removing a logged exercise deletes its sets; removing a workout deletes its entries/sets. Neither deletes catalogue records.

## Estimates and effort

React displays `estimates` supplied by the backend. It does not calculate Epley or RIR. **Toistoperusteinen e1RM** is a repetition-based estimate, not an RPE-adjusted or measured maximum. Effort warnings remain visible alongside the value. Unsuitable sets show a Finnish unavailability reason rather than zero. While a row has unsaved changes, old estimates are hidden until a successful response supplies current ones.

RPE is subjective; RPE 10 ≈ zero, 9 ≈ one and 8 ≈ two repetitions in reserve. RIR is an approximation. Open the estimate explanation for eligibility/limitations; [CALCULATIONS.md](CALCULATIONS.md) documents the Java policy and research boundaries. No personal record or progression claim is added by this UI.

## Source map and design choices

- `frontend/src/App.tsx`: hash routes, navigation guards, theme, skip link and status messages.
- `components/WorkoutList.tsx`: paged session navigation and creation.
- `components/ExerciseLibrary.tsx`: browse/search/create/edit and reusable workout picker.
- `components/WorkoutEditor.tsx`: saved workout tree, metadata, exercise notes, append and finish actions.
- `components/SetRow.tsx`: independent editable row, validation, explicit saves and server estimates.
- `components/Shared.tsx`: loading/error/empty states, pagination, original SVG icons and deletion confirmation.
- `api.ts`: bounded JSON requests and ProblemDetail handling; `types.ts`: explicit JSON interfaces.
- `hooks.ts`: cancellable GET resources and per-form dirty/pending tracking; `utils.ts`: locale and payload functions.
- `styles.css`: semantic tokens, responsive layouts, focus indicators, reduced-motion support and both themes.

No router, global state store, component framework or chart dependency was added. React state is sufficient for three small routes. The interface uses system fonts and SVG controls, with Roope's supplied full logo in the header and its symbol for the hero, favicon, empty states and exercise/workout icons. `public/logo.png` preserves the supplied image and its color profile; `logo-symbol.svg` and `favicon.svg` reproduce the simple mark with its sRGB red. A white logo surface preserves the supplied dark wordmark in both themes. `components/Footer.tsx` provides the author footer: Roberto Garcia, a factual Java/React project description, GitHub/Portfolio/Blogi links, and the current copyright year. Its bordered two-row layout wraps on small screens and uses the same theme tokens. There are no remote font/image requests. Only theme preference is stored in localStorage; system theme is used initially when no preference exists. Training drafts live only in memory.

## Failure and concurrency boundaries

Explicit saves show pending state and errors; failed input stays in its form. Requests time out after 15 seconds. Navigating within the app checks unsaved forms and blocks during writes; browser unload requests the browser's standard warning. Browser behavior can limit unload prompts, so save deliberately. Successful creation/deletion explicitly permits its own follow-up navigation after the server response, without depending on effect timing.

GET requests are cancelled on route/search changes and stale responses are ignored. Mutation responses update their changed subtree without resetting unrelated form drafts. Creating a logged exercise plus its first set is two requests: if the second fails, the saved exercise remains visible with an error and an explicit Add set recovery action.

A failed network response does not establish that a write failed. There is no automatic mutation retry, offline queue or autosave. Use **Päivitä tiedot**, inspect saved state and reapply only necessary changes; refresh warns before discarding current drafts. Multiple tabs/devices can overwrite the same resource because optimistic locking and conflict resolution are not implemented. Server validation/database constraints remain authoritative; TypeScript interfaces are not runtime validation.

## Accessibility and verification

Implemented: semantic landmarks/headings/labels, named icon controls even when text is hidden on small screens, keyboard focus rings, skip link, live save notices, inline alerts, confirmation cancellation focus, labelled progress, large numeric fields, at least 44px control targets, reduced motion and light/dark tokens. At <=440px set fields use two columns so full numeric values remain readable. Decoration has no semantic role.

Actual automated verification: 35 tests across five files, including decimal/range boundaries, payloads/date behavior, API error/204/network behavior, save/completion, deletion cancellation, dirty navigation, timestamp/draft preservation, partial creation recovery newly appended set focus, login/CSRF/logout, demo controls and same-account session-expiry draft recovery. axe-core reports zero semantic violations on empty dashboard and populated editor in jsdom; color-contrast is disabled there because jsdom has no rendered layout.

Actual manual verification: Safari desktop creation/picking/completion, keyboard movement from reps to RPE, next-set copying with cleared effort and new-field focus, light/dark rendering, responsive editor at 390px and 320px, and library at 320px. A real set at 82.25 kg × 6, RPE 8.5 returned RIR 1.5 and e1RM 98.70 with its submaximal warning. Token contrast calculations checked normal/muted/accent/status text pairs (minimum 5.52:1 among checked pairs) and input borders (3.65:1 light, 4.78:1 dark).

These checks do not certify complete WCAG 2.2 AA compliance. VoiceOver/full screen-reader flows, physical mobile touch/keyboard behavior and exhaustive browser/zoom combinations still need user evaluation. No screenshot fixtures or test records are presented as real training history. History analytics/charts are Milestone 6; two-account authentication is now implemented; production hosting still requires Milestone 7 review.


## Login and account privacy

`AuthGate.tsx` is now the production entry point, wrapping `App` only after identity is confirmed. The Finnish login selects `owner` or `demo`. Owner can log actual workouts; demo sees only invented seed workouts and shared built-ins, with mutations disabled/hidden. The backend independently enforces read-only demo access and account scoping. The theme remains available for both accounts. A sidebar logout action asks before discarding drafts and blocks during pending saves.

The browser keeps only an HttpOnly session cookie and an in-memory masked CSRF token. Password input is cleared after login attempts. No credentials/session tokens go in localStorage, URLs or React build-time environment variables. A 401, or a CSRF 403 followed by an unauthenticated identity check, opens reauthentication without replaying a mutation. Same-account drafts survive while hidden; switching account remounts the app and clears private screen state. Reloading the whole page still loses unsaved drafts. Saved private records are never used to seed demo data.
