import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { api, errorText } from "../api";
import { useAction, useDirty, useResource } from "../hooks";
import type {
  Exercise,
  Feedback,
  LoggedExercise,
  Workout,
  WorkoutInput,
  WorkoutSet,
} from "../types";
import {
  formatDate,
  instant,
  localDateTime,
  nextSet,
  nullable,
  workoutPayload,
} from "../utils";
import { ConfirmDelete, Empty, ErrorMessage, Icon, Loading } from "./Shared";
import { ExerciseLibrary } from "./ExerciseLibrary";
import { SetRow } from "./SetRow";

function metadata(workout: Workout) {
  return {
    date: workout.date,
    title: workout.title ?? "",
    notes: workout.notes ?? "",
    started: localDateTime(workout.startedAt),
    ended: localDateTime(workout.endedAt),
  };
}
function WorkoutDetails({
  workout,
  onUpdate,
  feedback,
}: {
  workout: Workout;
  onUpdate: (workout: Workout) => void;
  feedback: Feedback;
}) {
  const [form, setForm] = useState(() => metadata(workout));
  const initial = metadata(workout);
  const dirty = JSON.stringify(form) !== JSON.stringify(initial);
  useDirty(dirty, feedback);
  const action = useAction(feedback);
  function submit(event: FormEvent) {
    event.preventDefault();
    void action.run(async () => {
      // Preserve original second/microsecond precision if a displayed minute field was not edited.
      const startedAt =
        form.started === initial.started
          ? workout.startedAt
          : instant(form.started);
      const endedAt =
        form.ended === initial.ended ? workout.endedAt : instant(form.ended);
      if (
        endedAt &&
        (!startedAt ||
          new Date(endedAt).getTime() < new Date(startedAt).getTime())
      )
        throw new Error("Lopetusajan on oltava aloitusajan jälkeen tai sama.");
      const body: WorkoutInput = {
        date: form.date,
        title: nullable(form.title),
        notes: nullable(form.notes),
        startedAt,
        endedAt,
      };
      const updated = await api<Workout>(`/api/workouts/${workout.id}`, {
        method: "PUT",
        body,
      });
      setForm(metadata(updated));
      onUpdate(updated);
    }, "Harjoituksen tiedot tallennettu.");
  }
  return (
    <details className="panel workout-details">
      <summary>
        Harjoituksen tiedot ja muistiinpanot
        {dirty && <span className="badge accent-badge">Muokattu</span>}
      </summary>
      <form onSubmit={submit}>
        <fieldset disabled={action.busy}>
          <legend className="sr-only">Harjoituksen tiedot</legend>
          <div className="form-columns">
            <label>
              Nimi
              <input
                maxLength={100}
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="Harjoitus"
              />
            </label>
            <label>
              Päivämäärä
              <input
                type="date"
                required
                value={form.date}
                onChange={(e) => setForm({ ...form, date: e.target.value })}
              />
            </label>
          </div>
          <label>
            Muistiinpanot
            <textarea
              rows={3}
              maxLength={2000}
              value={form.notes}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
              placeholder="Miltä tänään tuntui?"
            />
          </label>
          <div className="form-columns">
            <label>
              Aloitusaika
              <input
                type="datetime-local"
                value={form.started}
                onChange={(e) => setForm({ ...form, started: e.target.value })}
              />
            </label>
            <label>
              Lopetusaika
              <input
                type="datetime-local"
                value={form.ended}
                onChange={(e) => setForm({ ...form, ended: e.target.value })}
              />
            </label>
          </div>
          <p className="help">
            Kellonajat näkyvät laitteesi paikallisessa ajassa. Voit jättää ne
            tyhjiksi.
          </p>
          <button className="button secondary" disabled={!dirty}>
            {action.busy ? "Tallennetaan…" : "Tallenna tiedot"}
          </button>
        </fieldset>
        {action.error && <ErrorMessage message={action.error} />}
      </form>
    </details>
  );
}
function LoggedNotes({
  entry,
  feedback,
  onUpdate,
}: {
  entry: LoggedExercise;
  feedback: Feedback;
  onUpdate: (entry: LoggedExercise) => void;
}) {
  const [notes, setNotes] = useState(entry.notes ?? "");
  const dirty = notes !== (entry.notes ?? "");
  useDirty(dirty, feedback);
  const action = useAction(feedback);
  return (
    <details className="entry-notes">
      <summary>Liikkeen muistiinpanot{dirty ? " · muokattu" : ""}</summary>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          void action.run(async () => {
            const updated = await api<LoggedExercise>(
              `/api/workout-exercises/${entry.id}`,
              {
                method: "PUT",
                body: {
                  displayOrder: entry.displayOrder,
                  notes: nullable(notes),
                },
              },
            );
            setNotes(updated.notes ?? "");
            onUpdate(updated);
          }, "Liikkeen muistiinpanot tallennettu.");
        }}
      >
        <label>
          <span className="sr-only">Muistiinpanot: {entry.exercise.name}</span>
          <textarea
            maxLength={2000}
            rows={2}
            disabled={action.busy}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </label>
        <button className="text-button" disabled={!dirty || action.busy}>
          Tallenna muistiinpanot
        </button>
      </form>
      {action.error && <ErrorMessage message={action.error} />}
    </details>
  );
}
function LoggedCard({
  entry,
  index,
  onUpdate,
  onRemove,
  feedback,
  focusSet,
}: {
  entry: LoggedExercise;
  index: number;
  onUpdate: (update: (entry: LoggedExercise) => LoggedExercise) => void;
  onRemove: () => void;
  feedback: Feedback;
  focusSet: number | null;
}) {
  const action = useAction(feedback);
  const addRef = useRef<HTMLButtonElement>(null);
  const focusedSet = useRef<number | null>(null);
  useEffect(() => {
    if (focusSet && focusedSet.current !== focusSet) {
      const input = document.getElementById(`weight-${focusSet}`);
      if (input) {
        input.focus();
        focusedSet.current = focusSet;
      }
    }
  }, [focusSet, entry.sets]);
  function addSet() {
    void action.run(async () => {
      const set = await api<WorkoutSet>(
        `/api/workout-exercises/${entry.id}/sets`,
        { method: "POST", body: nextSet(entry.sets) },
      );
      onUpdate((current) => ({ ...current, sets: [...current.sets, set] }));
      requestAnimationFrame(() =>
        document.getElementById(`weight-${set.id}`)?.focus(),
      );
    }, "Uusi sarja lisätty.");
  }
  return (
    <section
      className="exercise-card panel"
      aria-labelledby={`entry-${entry.id}`}
    >
      <header className="exercise-card-header">
        <span className="entry-index">
          {String(index + 1).padStart(2, "0")}
        </span>
        <div>
          <h2 id={`entry-${entry.id}`}>{entry.exercise.name}</h2>
          <p>
            {[entry.exercise.primaryMuscleGroup, entry.exercise.equipment]
              .filter(Boolean)
              .join(" · ") || "Oma liike"}
          </p>
        </div>
        <ConfirmDelete
          label={`Poista ${entry.exercise.name} harjoituksesta`}
          question="Poistetaanko liike ja sen sarjat tästä harjoituksesta?"
          disabled={action.busy}
          onConfirm={() =>
            action.run(async () => {
              await api<void>(`/api/workout-exercises/${entry.id}`, {
                method: "DELETE",
              });
              onRemove();
            }, "Liike ja sen sarjat poistettu harjoituksesta.")
          }
        />
      </header>
      {entry.sets.length === 0 && (
        <p className="help no-sets">
          Lisää ensimmäinen sarja ja kirjaa paino sekä toistot.
        </p>
      )}
      {entry.sets.map((set, number) => (
        <SetRow
          key={set.id}
          set={set}
          number={number + 1}
          feedback={feedback}
          onUpdate={(updated) =>
            onUpdate((current) => ({
              ...current,
              sets: current.sets.map((s) =>
                s.id === updated.id ? updated : s,
              ),
            }))
          }
          onDelete={(id) => {
            onUpdate((current) => ({
              ...current,
              sets: current.sets.filter((s) => s.id !== id),
            }));
            requestAnimationFrame(() => addRef.current?.focus());
          }}
        />
      ))}
      <button
        ref={addRef}
        className="add-set"
        disabled={action.busy}
        onClick={addSet}
      >
        <Icon name="plus" size={18} />
        {action.busy ? "Lisätään…" : "Lisää sarja"}
        <small>edellinen paino ja toistot valmiiksi</small>
      </button>
      {action.error && <ErrorMessage message={action.error} />}
      <LoggedNotes
        entry={entry}
        feedback={feedback}
        onUpdate={(updated) =>
          onUpdate((current) => ({
            ...current,
            notes: updated.notes,
            displayOrder: updated.displayOrder,
          }))
        }
      />
    </section>
  );
}

export function WorkoutEditor({
  id,
  feedback,
  navigate,
  hasDirty,
  hasBusy,
}: {
  id: number;
  feedback: Feedback;
  navigate: (path: string, afterSave?: boolean) => void;
  hasDirty: boolean;
  hasBusy: boolean;
}) {
  const resource = useResource<Workout>(`/api/workouts/${id}`);
  const [picker, setPicker] = useState(false);
  const [focusSet, setFocusSet] = useState<number | null>(null);
  const action = useAction(feedback);
  const addExerciseRef = useRef<HTMLButtonElement>(null);
  const workout = resource.data;
  function mergeMetadata(updated: Workout) {
    resource.setData((current) =>
      current ? { ...current, ...workoutPayload(updated) } : updated,
    );
  }
  function updateEntry(
    entryId: number,
    update: (entry: LoggedExercise) => LoggedExercise,
  ) {
    resource.setData((current) =>
      current
        ? {
            ...current,
            exercises: current.exercises.map((e) =>
              e.id === entryId ? update(e) : e,
            ),
          }
        : current,
    );
  }
  async function selectExercise(exercise: Exercise) {
    if (!workout) return;
    const entry = await api<LoggedExercise>(`/api/workouts/${id}/exercises`, {
      method: "POST",
      body: { exerciseId: exercise.id, notes: null },
    });
    resource.setData((current) =>
      current
        ? { ...current, exercises: [...current.exercises, entry] }
        : current,
    );
    // Persist the parent first. If set creation fails, the visible empty entry remains retryable.
    try {
      const set = await api<WorkoutSet>(
        `/api/workout-exercises/${entry.id}/sets`,
        { method: "POST", body: {} },
      );
      updateEntry(entry.id, (current) => ({ ...current, sets: [set] }));
      setFocusSet(set.id);
    } catch (error) {
      setPicker(false);
      action.setError(
        `Liike lisättiin, mutta ensimmäistä sarjaa ei saatu lisättyä. Käytä liikkeen Lisää sarja -painiketta. ${errorText(error)}`,
      );
      return;
    }
    setPicker(false);
    feedback.onNotice(`${exercise.name} ja ensimmäinen sarja lisätty.`);
  }
  const completed =
    workout?.exercises.flatMap((e) => e.sets).filter((s) => s.completed)
      .length ?? 0;
  const total =
    workout?.exercises.reduce((count, e) => count + e.sets.length, 0) ?? 0;
  return (
    <>
      <a className="back-link" href="#/workouts">
        ← Kaikki harjoitukset
      </a>
      <header className="section-heading editor-heading">
        <div>
          <p className="eyebrow">
            {workout ? formatDate(workout.date) : "HARJOITUS"}
          </p>
          <h1 tabIndex={-1}>{workout?.title || "Harjoitus"}</h1>
        </div>
        {workout && (
          <span className={`badge ${workout.endedAt ? "" : "accent-badge"}`}>
            {workout.endedAt ? "Päätetty harjoitus" : "Avoin harjoitus"}
          </span>
        )}
      </header>
      {resource.loading ? (
        <Loading />
      ) : resource.error ? (
        <ErrorMessage message={resource.error} retry={resource.refresh} />
      ) : (
        workout && (
          <div className="editor-layout">
            <div className="editor-main">
              <WorkoutDetails
                key={`${id}-${workout.startedAt}-${workout.endedAt}`}
                workout={workout}
                feedback={feedback}
                onUpdate={mergeMetadata}
              />
              <div className="section-heading compact">
                <h2>
                  Liikkeet{" "}
                  <span className="muted">/ {workout.exercises.length}</span>
                </h2>
                <button
                  className="text-button"
                  ref={addExerciseRef}
                  onClick={() => setPicker((p) => !p)}
                >
                  <Icon name="plus" size={18} />
                  Lisää liike
                </button>
              </div>
              {picker && (
                <ExerciseLibrary
                  selecting
                  feedback={feedback}
                  onSelect={selectExercise}
                  onClose={() => {
                    setPicker(false);
                    addExerciseRef.current?.focus();
                  }}
                />
              )}
              {workout.exercises.length === 0 && !picker && (
                <Empty title="Mistä aloitetaan?">
                  <p>
                    Valitse harjoituksen ensimmäinen liike. Voit lisätä lisää
                    sitä mukaa kuin etenet.
                  </p>
                  <button
                    className="button primary"
                    onClick={() => setPicker(true)}
                  >
                    <Icon name="plus" />
                    Valitse liike
                  </button>
                </Empty>
              )}
              {workout.exercises.map((entry, index) => (
                <LoggedCard
                  key={entry.id}
                  entry={entry}
                  index={index}
                  feedback={feedback}
                  focusSet={focusSet}
                  onUpdate={(update) => updateEntry(entry.id, update)}
                  onRemove={() => {
                    resource.setData((current) =>
                      current
                        ? {
                            ...current,
                            exercises: current.exercises.filter(
                              (e) => e.id !== entry.id,
                            ),
                          }
                        : current,
                    );
                    requestAnimationFrame(() =>
                      addExerciseRef.current?.focus(),
                    );
                  }}
                />
              ))}
              {workout.exercises.length > 0 && (
                <button
                  className="add-exercise"
                  onClick={() => {
                    setPicker(true);
                    window.scrollTo({ top: 0, behavior: "instant" });
                  }}
                >
                  <Icon name="plus" />
                  Lisää seuraava liike
                </button>
              )}
            </div>
            <aside
              className="editor-aside"
              aria-label="Harjoituksen yhteenveto ja ohjeet"
            >
              <section className="panel session-summary">
                <span className="eyebrow">TÄMÄ HARJOITUS</span>
                <div className="summary-number">
                  {completed}
                  <span> / {total}</span>
                </div>
                <p>sarjaa valmiina</p>
                <progress
                  value={completed}
                  max={Math.max(total, 1)}
                  aria-label={`${completed} sarjaa valmiina, yhteensä ${total}`}
                />
                <div className="summary-line">
                  <span>Liikkeitä</span>
                  <strong>{workout.exercises.length}</strong>
                </div>
                {hasDirty && (
                  <p className="dirty-notice">
                    Tallentamattomia muutoksia. Tallenna ne sarjan tai tietojen
                    painikkeella.
                  </p>
                )}
                {!workout.endedAt ? (
                  <button
                    className="button primary full"
                    disabled={hasDirty || hasBusy}
                    onClick={() =>
                      void action.run(
                        async () => {
                          const body = {
                            ...workoutPayload(workout),
                            ...(workout.startedAt
                              ? { endedAt: new Date().toISOString() }
                              : { startedAt: new Date().toISOString() }),
                          };
                          mergeMetadata(
                            await api<Workout>(`/api/workouts/${id}`, {
                              method: "PUT",
                              body,
                            }),
                          );
                        },
                        workout.startedAt
                          ? "Harjoitus päätetty. Hyvä työ!"
                          : "Ajanotto aloitettu.",
                      )
                    }
                  >
                    {workout.startedAt ? "Päätä harjoitus" : "Aloita ajanotto"}
                    <Icon name={workout.startedAt ? "check" : "arrow"} />
                  </button>
                ) : (
                  <p className="finished-note">
                    <Icon name="check" />
                    Harjoitus päätetty. Voit yhä muokata kirjauksia.
                  </p>
                )}
                {action.error && <ErrorMessage message={action.error} />}
              </section>
              <section className="guide">
                <h2>Rasittavuus omilla ehdoillasi.</h2>
                <p>RPE kertoo, kuinka raskaalta sarja tuntui.</p>
                <dl>
                  <div>
                    <dt>RPE 10</dt>
                    <dd>≈ 0 toistoa varastossa</dd>
                  </div>
                  <div>
                    <dt>RPE 9</dt>
                    <dd>≈ 1 toisto varastossa</dd>
                  </div>
                  <div>
                    <dt>RPE 8</dt>
                    <dd>≈ 2 toistoa varastossa</dd>
                  </div>
                </dl>
                <p className="help">
                  RIR on omaan arvioosi perustuva likiarvo. RPE:n voi jättää
                  tyhjäksi.
                </p>
              </section>
              <details className="estimate-guide">
                <summary>Mitä toistoperusteinen e1RM tarkoittaa?</summary>
                <p>
                  Toistomäärään perustuva e1RM arvioi yhden toiston kuormaa. Se
                  ei ole RPE-korjattu eikä mitattu ennätys. Arvio näytetään
                  valmiista työsarjoista, joissa on positiivinen kuorma ja 1–10
                  toistoa.
                </p>
                <p>
                  Vertaa vain samaa liikettä ja välinettä samalla
                  kirjaustavalla. Käsipainojen kuormaa ei kaksinkertaisteta
                  automaattisesti.
                </p>
              </details>
              <button
                className="text-button muted"
                disabled={hasBusy}
                onClick={() => {
                  if (
                    !hasDirty ||
                    window.confirm(
                      "Päivitys hylkää tallentamattomat muutokset. Päivitetäänkö tiedot?",
                    )
                  )
                    resource.refresh();
                }}
              >
                Päivitä tiedot
              </button>
              <div className="workout-delete">
                <ConfirmDelete
                  label="Poista koko harjoitus"
                  question="Poistetaanko harjoitus ja kaikki sen sarjat?"
                  disabled={hasBusy}
                  onConfirm={() =>
                    action.run(async () => {
                      await api<void>(`/api/workouts/${id}`, {
                        method: "DELETE",
                      });
                      resource.setData(null);
                      navigate("/workouts", true);
                    }, "Harjoitus poistettu.")
                  }
                />
              </div>
            </aside>
          </div>
        )
      )}
    </>
  );
}
