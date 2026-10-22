import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { Feedback } from "./types";
import { ExerciseLibrary } from "./components/ExerciseLibrary";
import { WorkoutEditor } from "./components/WorkoutEditor";
import { WorkoutList } from "./components/WorkoutList";
import { Icon } from "./components/Shared";
import type { Account } from "./AuthGate";
import { Footer } from "./components/Footer";

function readRoute() {
  return window.location.hash.slice(1) || "/workouts";
}
export function initialTheme(): "light" | "dark" {
  try {
    const saved = localStorage.getItem("puntti-urpo-theme");
    if (saved === "light" || saved === "dark") return saved;
  } catch {
    /* Private storage can be unavailable. */
  }
  return typeof window.matchMedia === "function" &&
    window.matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
}
export function App({
  account = { username: "owner", readOnly: false },
  onLogout,
}: {
  account?: Account;
  onLogout?: () => Promise<void>;
}) {
  const [route, setRoute] = useState(readRoute);
  const routeRef = useRef(route);
  const [theme, setTheme] = useState(initialTheme);
  const [dirty, setDirty] = useState<Record<string, boolean>>({});
  const [busy, setBusy] = useState<Record<string, boolean>>({});
  const dirtyRef = useRef(dirty);
  const busyRef = useRef(busy);
  const [notice, setNotice] = useState("");
  const onDirty = useCallback((key: string, value: boolean) => {
    dirtyRef.current = { ...dirtyRef.current, [key]: value };
    setDirty((previous) =>
      previous[key] === value ? previous : { ...previous, [key]: value },
    );
  }, []);
  const onBusy = useCallback((key: string, value: boolean) => {
    busyRef.current = { ...busyRef.current, [key]: value };
    setBusy((previous) =>
      previous[key] === value ? previous : { ...previous, [key]: value },
    );
  }, []);
  const onNotice = useCallback((message: string) => setNotice(message), []);
  const feedback: Feedback = useMemo(
    () => ({ onDirty, onBusy, onNotice }),
    [onDirty, onBusy, onNotice],
  );
  const savedDestination = useRef<string | null>(null);
  // Only successful create/delete flows may leave while React settles form state.
  const navigate = useCallback((path: string, afterSave = false) => {
    savedDestination.current = afterSave ? path : null;
    window.location.hash = path;
  }, []);
  useEffect(() => {
    function changed() {
      const next = readRoute();
      if (next === routeRef.current) return;
      const afterSave = savedDestination.current === next;
      savedDestination.current = null;
      if (!afterSave && Object.values(busyRef.current).some(Boolean)) {
        window.history.replaceState(null, "", `#${routeRef.current}`);
        setNotice("Odota tallennuksen valmistumista ennen siirtymistä.");
        return;
      }
      if (
        !afterSave &&
        Object.values(dirtyRef.current).some(Boolean) &&
        !window.confirm(
          "Sinulla on tallentamattomia muutoksia. Hylätäänkö muutokset ja siirrytään?",
        )
      ) {
        window.history.replaceState(null, "", `#${routeRef.current}`);
        return;
      }
      dirtyRef.current = {};
      busyRef.current = {};
      setDirty({});
      setBusy({});
      setNotice("");
      routeRef.current = next;
      setRoute(next);
    }
    function beforeUnload(event: BeforeUnloadEvent) {
      if (
        Object.values(dirtyRef.current).some(Boolean) ||
        Object.values(busyRef.current).some(Boolean)
      ) {
        event.preventDefault();
        event.returnValue = "";
      }
    }
    window.addEventListener("hashchange", changed);
    window.addEventListener("beforeunload", beforeUnload);
    return () => {
      window.removeEventListener("hashchange", changed);
      window.removeEventListener("beforeunload", beforeUnload);
    };
  }, []);
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
  }, [theme]);
  useEffect(() => {
    document.title = `${route === "/exercises" ? "Liikekirjasto" : route === "/workouts" ? "Harjoitukset" : "Harjoitus"} · Puntti-Urpo`;
    document.querySelector<HTMLElement>("h1")?.focus({ preventScroll: true });
    window.scrollTo(0, 0);
  }, [route]);
  function toggleTheme() {
    const next = theme === "light" ? "dark" : "light";
    setTheme(next);
    try {
      localStorage.setItem("puntti-urpo-theme", next);
    } catch {
      /* Theme still works without storage. */
    }
  }
  const match = /^\/workouts\/([1-9]\d*)$/.exec(route);
  const id = match ? Number(match[1]) : null;
  const validId = id !== null && Number.isSafeInteger(id);
  return (
    <div className="app-shell">
      <a
        href="#main"
        className="skip-link"
        onClick={(event) => {
          event.preventDefault();
          document.getElementById("main")?.focus();
        }}
      >
        Siirry sisältöön
      </a>
      <aside className="sidebar" aria-label="Sovelluksen valinnat">
        <a
          className="brand"
          href="#/workouts"
          aria-label="Puntti-Urpo, harjoitukset"
        >
          <img
            className="brand-logo"
            src="/logo.png"
            width="452"
            height="178"
            alt="Puntti-Urpo — Tee. Kirjaa. Toista."
          />
        </a>
        <nav className="main-nav" aria-label="Päänavigaatio">
          <a
            href="#/workouts"
            aria-current={route.startsWith("/workouts") ? "page" : undefined}
          >
            <Icon name="list" />
            <span>Harjoitukset</span>
            <Icon name="arrow" size={16} />
          </a>
          <a
            href="#/exercises"
            aria-current={route === "/exercises" ? "page" : undefined}
          >
            <Icon name="grid" />
            <span>Liikkeet</span>
            <Icon name="arrow" size={16} />
          </a>
        </nav>
        <div className="sidebar-bottom">
          <span className="small-stamp">YKSI SARJA KERRALLAAN.</span>
          <button
            className="theme-button"
            onClick={toggleTheme}
            aria-label={theme === "light" ? "Tumma teema" : "Vaalea teema"}
          >
            <Icon name={theme === "light" ? "moon" : "sun"} />
            <span>{theme === "light" ? "Tumma teema" : "Vaalea teema"}</span>
          </button>
          <p>
            {account.readOnly
              ? "Demotili · vain luku"
              : "Oma tili · yksityinen"}
          </p>
          {onLogout && (
            <button
              className="text-button"
              disabled={Object.values(busy).some(Boolean)}
              onClick={() => {
                if (
                  Object.values(dirtyRef.current).some(Boolean) &&
                  !window.confirm(
                    "Hylätäänkö tallentamattomat muutokset ja kirjaudutaan ulos?",
                  )
                )
                  return;
                void onLogout();
              }}
            >
              Kirjaudu ulos
            </button>
          )}
        </div>
      </aside>
      <main id="main" tabIndex={-1}>
        <div className="topline">
          <span className="topline-label">HARJOITTELU / OMA PÄIVÄKIRJA</span>
          <span className="version-label">
            <i aria-hidden="true" />
            Paikallinen versio
          </span>
        </div>
        {account.readOnly && (
          <p className="demo-banner" role="status">
            Demotili — vain kuvitteellisia harjoituksia. Tietoja ei voi muuttaa.
          </p>
        )}
        {route === "/workouts" ? (
          <WorkoutList
            feedback={feedback}
            navigate={navigate}
            readOnly={account.readOnly}
          />
        ) : route === "/exercises" ? (
          <ExerciseLibrary feedback={feedback} readOnly={account.readOnly} />
        ) : validId ? (
          <fieldset disabled={account.readOnly} className="read-only-workout">
            <WorkoutEditor
              key={id}
              id={id}
              feedback={feedback}
              navigate={navigate}
              hasDirty={Object.values(dirty).some(Boolean)}
              hasBusy={Object.values(busy).some(Boolean)}
            />
          </fieldset>
        ) : (
          <section className="empty">
            <h1 tabIndex={-1}>Sivua ei löytynyt</h1>
            <a className="button primary" href="#/workouts">
              Palaa harjoituksiin
            </a>
          </section>
        )}
        <Footer />
      </main>
      <div
        className="sr-only"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        {notice}
      </div>
    </div>
  );
}
