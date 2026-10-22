import { useState } from "react";
import type { FormEvent } from "react";
import { api } from "../api";
import { useAction, useDirty, useResource } from "../hooks";
import type { Feedback, Page, Workout, WorkoutSummary } from "../types";
import { formatDate, nullable, todayLocal } from "../utils";
import { Empty, ErrorMessage, Icon, Loading, Pager } from "./Shared";

export function WorkoutList({
  feedback,
  navigate,
  readOnly = false,
}: {
  feedback: Feedback;
  readOnly?: boolean;
  navigate: (path: string, afterSave?: boolean) => void;
}) {
  const [page, setPage] = useState(0);
  const resource = useResource<Page<WorkoutSummary>>(
    `/api/workouts?page=${page}&size=8`,
  );
  const [creating, setCreating] = useState(false);
  const [title, setTitle] = useState("");
  const action = useAction(feedback);
  useDirty(creating && title.length > 0, feedback);
  function start(event: FormEvent) {
    event.preventDefault();
    void action.run(async () => {
      const workout = await api<Workout>("/api/workouts", {
        method: "POST",
        body: {
          date: todayLocal(),
          title: nullable(title),
          notes: null,
          startedAt: new Date().toISOString(),
          endedAt: null,
        },
      });
      setTitle("");
      setCreating(false);
      // The successful response authorizes leaving this saved creation form.
      navigate(`/workouts/${workout.id}`, true);
    }, "Uusi harjoitus aloitettu.");
  }
  return (
    <>
      <header className="section-heading">
        <div>
          <p className="eyebrow">OMA HARJOITUSPÄIVÄKIRJASI</p>
          <h1 tabIndex={-1}>Harjoitus kerrallaan.</h1>
        </div>
        <span className="today">{formatDate(todayLocal())}</span>
      </header>
      <section className="start-panel">
        <div className="start-copy">
          <span className="label-dot">SYTTYY NYT VÄHÄN SIIHEN</span>
          <h2>
            Simple.
            <br />
            Not easy.
          </h2>
          <p>
            Kirjaa sarjat, RPE ja keskity tekemiseen. Jokainen toisto on yhtä tärkeä.
          </p>
          {!creating && !readOnly && (
            <button
              className="button primary large"
              onClick={() => setCreating(true)}
            >
              <Icon name="plus" />
              Aloita harjoitus
            </button>
          )}
          {creating && (
            <form className="start-form" onSubmit={start}>
              <label>
                Harjoituksen nimi <small>valinnainen</small>
                <input
                  autoFocus
                  maxLength={100}
                  placeholder="Esim. Jalkapäivä"
                  value={title}
                  disabled={action.busy}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </label>
              <div className="button-row">
                <button className="button primary" disabled={action.busy}>
                  {action.busy ? "Aloitetaan…" : "Aloita nyt"}
                  <Icon name="arrow" />
                </button>
                <button
                  className="button secondary"
                  type="button"
                  disabled={action.busy}
                  onClick={() => {
                    setCreating(false);
                    setTitle("");
                  }}
                >
                  Peruuta
                </button>
              </div>
            </form>
          )}
          {action.error && <ErrorMessage message={action.error} />}
        </div>
        <div className="start-art" aria-hidden="true">
          <img src="/logo-symbol.svg" width="180" height="180" alt="" />
        </div>
      </section>
      <section className="workout-list">
        <div className="section-heading compact">
          <h2>Harjoituksesi</h2>
          {resource.data && (
            <span className="muted">
              {resource.data.totalElements} kirjattua harjoitusta
            </span>
          )}
        </div>
        {resource.loading ? (
          <Loading />
        ) : resource.error ? (
          <ErrorMessage message={resource.error} retry={resource.refresh} />
        ) : (
          resource.data && (
            <>
              {resource.data.items.length === 0 ? (
                <Empty title="Ensimmäinen harjoitus odottaa">
                  <p>
                    Aloita harjoitus yllä ja lisää haluamasi liikkeet.
                    Kirjaaminen alkaa yhdestä sarjasta.
                  </p>
                </Empty>
              ) : (
                <div className="session-list">
                  {resource.data.items.map((workout) => (
                    <a
                      key={workout.id}
                      className="session-link"
                      href={`#/workouts/${workout.id}`}
                    >
                      <span className="session-icon">
                        <img
                          src="/logo-symbol.svg"
                          width={25}
                          height={25}
                          alt=""
                        />
                      </span>
                      <span className="session-copy">
                        <strong>{workout.title || "Harjoitus"}</strong>
                        <span>{formatDate(workout.date)}</span>
                      </span>
                      <span
                        className={`badge ${workout.endedAt ? "" : "accent-badge"}`}
                      >
                        {workout.endedAt ? "Päätetty" : "Avoin"}
                      </span>
                      <Icon name="arrow" />
                    </a>
                  ))}
                </div>
              )}
              <Pager
                page={resource.data.page}
                totalPages={resource.data.totalPages}
                onChange={setPage}
              />
            </>
          )
        )}
      </section>
      <div className="quiet-note">
        <span>OMALLA TAHDILLA.</span>
        <p>Sinä nostat. Puntti-Urpo pitää kirjaa.</p>
      </div>
    </>
  );
}
