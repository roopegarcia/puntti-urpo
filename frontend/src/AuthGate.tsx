import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { App, initialTheme } from "./App";
import { api, errorText, setCsrf } from "./api";
import { Footer } from "./components/Footer";

export interface Account {
  username: string;
  readOnly: boolean;
}

async function refreshCsrf() {
  const result = await api<{ token: string; headerName: string }>(
    "/api/auth/csrf",
  );
  setCsrf(result);
}

export function AuthGate() {
  const [account, setAccount] = useState<Account | null>(null);
  const [loading, setLoading] = useState(true);
  const [expired, setExpired] = useState(false);
  const [username, setUsername] = useState("owner");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    const theme = initialTheme();
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    let active = true;
    void (async () => {
      try {
        await refreshCsrf();
        const me = await api<Account>("/api/auth/me");
        if (active) setAccount(me);
      } catch (failure) {
        if (
          active &&
          !(
            failure instanceof Error &&
            "status" in failure &&
            failure.status === 401
          )
        )
          setError(errorText(failure));
      } finally {
        if (active) setLoading(false);
      }
    })();
    const sessionExpired = () => {
      setExpired(true);
      setPassword("");
    };
    window.addEventListener("session-expired", sessionExpired);
    return () => {
      active = false;
      window.removeEventListener("session-expired", sessionExpired);
    };
  }, []);
  async function login(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      await refreshCsrf();
      await api("/api/auth/login", {
        method: "POST",
        form: new URLSearchParams({ username, password }),
      });
      setPassword("");
      await refreshCsrf();
      const me = await api<Account>("/api/auth/me");
      // A different account remounts App; another user's data/drafts never survive account switching.
      if (account && account.username !== me.username)
        window.history.replaceState(null, "", "#/workouts");
      setAccount(me);
      setExpired(false);
    } catch (failure) {
      setPassword("");
      setError(errorText(failure));
    } finally {
      setBusy(false);
    }
  }
  async function logout() {
    setError("");
    try {
      await refreshCsrf();
      await api("/api/auth/logout", { method: "POST" });
      setCsrf(null);
      setAccount(null);
      setExpired(false);
      setPassword("");
      window.history.replaceState(null, "", "#/workouts");
    } catch (failure) {
      setError(errorText(failure));
    }
  }
  if (loading)
    return (
      <div className="auth-loading" role="status">
        Tarkistetaan kirjautumista…
      </div>
    );
  return (
    <>
      {account && (
        <div hidden={expired}>
          <App key={account.username} account={account} onLogout={logout} />
        </div>
      )}
      {(!account || expired) && (
        <main className="login-page">
          <section className="login-panel" aria-labelledby="login-heading">
            <img
              className="brand-logo"
              src="/logo.png"
              width="452"
              height="178"
              alt="Puntti-Urpo — Tee. Kirjaa. Toista."
            />
            <p className="eyebrow">OMA TAHTI. OMAT KIRJAUKSET.</p>
            <h1 id="login-heading">
              {expired ? "Kirjaudu uudelleen" : "Tervetuloa takaisin."}
            </h1>
            <p>
              {expired
                ? "Istuntosi päättyi. Saman tilin tallentamattomat muutokset säilyvät tässä välilehdessä."
                : "Oma tili harjoitteluun. Demotili sovellukseen tutustumiseen."}
            </p>
            <form onSubmit={login}>
              <fieldset disabled={busy}>
                <label>
                  Käyttäjätunnus
                  <select
                    autoComplete="username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                  >
                    <option value="owner">Oma tili (owner)</option>
                    <option value="demo">Demotili (demo)</option>
                  </select>
                </label>
                <label>
                  Salasana
                  <input
                    type="password"
                    autoComplete="current-password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                </label>
                <button className="button primary full">
                  {busy ? "Kirjaudutaan…" : "Kirjaudu sisään"}
                </button>
              </fieldset>
            </form>
            {error && (
              <p className="field-error" role="alert">
                {error}
              </p>
            )}
            <p className="help">
              Demotilillä on vain kuvitteellista aineistoa ja lukuoikeus. Uusia
              tilejä ei voi rekisteröidä.
            </p>
          </section>
          <Footer />
        </main>
      )}
      {account && !expired && error && (
        <p className="auth-error" role="alert">
          {error}
        </p>
      )}
    </>
  );
}
