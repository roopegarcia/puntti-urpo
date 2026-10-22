import { useState } from "react";
import type { FormEvent } from "react";
import { api } from "../api";
import { useAction, useDirty, useResource } from "../hooks";
import type { Exercise, ExerciseInput, Feedback, Page } from "../types";
import { nullable } from "../utils";
import { Empty, ErrorMessage, Icon, Loading, Pager } from "./Shared";

function ExerciseForm({
  exercise,
  onSaved,
  onCancel,
  feedback,
}: {
  exercise: Exercise | null;
  onSaved: () => void;
  onCancel: () => void;
  feedback: Feedback;
}) {
  const initial = {
    name: exercise?.name ?? "",
    category: exercise?.category ?? "",
    primaryMuscleGroup: exercise?.primaryMuscleGroup ?? "",
    equipment: exercise?.equipment ?? "",
    notes: exercise?.notes ?? "",
  };
  const [form, setForm] = useState(initial);
  const action = useAction(feedback);
  useDirty(JSON.stringify(form) !== JSON.stringify(initial), feedback);
  function submit(event: FormEvent) {
    event.preventDefault();
    void action.run(
      async () => {
        if (!form.name.trim()) throw new Error("Anna liikkeelle nimi.");
        const body: ExerciseInput = {
          name: form.name.trim(),
          category: nullable(form.category),
          primaryMuscleGroup: nullable(form.primaryMuscleGroup),
          equipment: nullable(form.equipment),
          notes: nullable(form.notes),
        };
        await api<Exercise>(
          exercise ? `/api/exercises/${exercise.id}` : "/api/exercises",
          { method: exercise ? "PUT" : "POST", body },
        );
        onSaved();
      },
      exercise ? "Oma liike päivitetty." : "Oma liike lisätty.",
    );
  }
  return (
    <form className="panel edit-form" onSubmit={submit}>
      <h2>{exercise ? "Muokkaa omaa liikettä" : "Lisää oma liike"}</h2>
      <fieldset disabled={action.busy}>
        <legend className="sr-only">Liikkeen tiedot</legend>
        <label>
          Nimi <span className="required">pakollinen</span>
          <input
            autoFocus
            required
            maxLength={100}
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </label>
        <div className="form-columns">
          <label>
            Väline
            <input
              maxLength={50}
              value={form.equipment}
              onChange={(e) => setForm({ ...form, equipment: e.target.value })}
            />
          </label>
          <label>
            Lihasryhmä
            <input
              maxLength={50}
              value={form.primaryMuscleGroup}
              onChange={(e) =>
                setForm({ ...form, primaryMuscleGroup: e.target.value })
              }
            />
          </label>
        </div>
        <label>
          Luokka
          <input
            maxLength={50}
            value={form.category}
            onChange={(e) => setForm({ ...form, category: e.target.value })}
          />
        </label>
        <label>
          Muistiinpanot
          <textarea
            maxLength={2000}
            rows={3}
            value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
          />
        </label>
        <div className="button-row">
          <button className="button primary" type="submit">
            {action.busy ? "Tallennetaan…" : "Tallenna liike"}
          </button>
          <button className="button secondary" type="button" onClick={onCancel}>
            Peruuta
          </button>
        </div>
      </fieldset>
      {action.error && <ErrorMessage message={action.error} />}
    </form>
  );
}

export function ExerciseLibrary({
  feedback,
  readOnly = false,
  onSelect,
  selecting = false,
  onClose,
}: {
  feedback: Feedback;
  readOnly?: boolean;
  onSelect?: (exercise: Exercise) => Promise<void>;
  selecting?: boolean;
  onClose?: () => void;
}) {
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const resource = useResource<Page<Exercise>>(
    `/api/exercises?q=${encodeURIComponent(query)}&page=${page}&size=12`,
  );
  const [form, setForm] = useState<Exercise | "new" | null>(null);
  const action = useAction(feedback);
  function searchSubmit(event: FormEvent) {
    event.preventDefault();
    setPage(0);
    setQuery(search.trim());
  }
  return (
    <section
      className={selecting ? "exercise-picker panel" : ""}
      aria-label={selecting ? "Lisää liike harjoitukseen" : "Liikekirjasto"}
    >
      <div className="section-heading">
        <div>
          <p className="eyebrow">
            {selecting ? "HARJOITUKSEEN" : "OMA TYÖKALUPAKKISI"}
          </p>
          {selecting ? (
            <h2>Valitse seuraava liike</h2>
          ) : (
            <h1 tabIndex={-1}>Liikekirjasto</h1>
          )}
        </div>
        <div className="button-row">
          {!selecting && !readOnly && (
            <button className="button primary" onClick={() => setForm("new")}>
              <Icon name="plus" />
              Oma liike
            </button>
          )}
          {onClose && (
            <button
              type="button"
              className="button secondary"
              onClick={onClose}
            >
              Sulje valinta
            </button>
          )}
        </div>
      </div>
      {!selecting && (
        <p className="lead">
          Tutut liikkeet, omat variaatiot. Lisää vain se, mitä tarvitset.
        </p>
      )}
      {form && (
        <ExerciseForm
          key={form === "new" ? "new" : form.id}
          exercise={form === "new" ? null : form}
          feedback={feedback}
          onSaved={() => {
            setForm(null);
            resource.refresh();
          }}
          onCancel={() => setForm(null)}
        />
      )}
      <form className="search" onSubmit={searchSubmit}>
        <label
          htmlFor={selecting ? "picker-search" : "exercise-search"}
          className="sr-only"
        >
          Hae liikettä nimellä
        </label>
        <input
          id={selecting ? "picker-search" : "exercise-search"}
          type="search"
          placeholder="Hae liikettä nimellä…"
          maxLength={100}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button type="submit" className="button secondary">
          Hae
        </button>
      </form>
      {resource.loading ? (
        <Loading />
      ) : resource.error ? (
        <ErrorMessage message={resource.error} retry={resource.refresh} />
      ) : (
        resource.data && (
          <>
            <p className="result-count">
              {resource.data.totalElements}{" "}
              {resource.data.totalElements === 1 ? "liike" : "liikettä"}
              {query ? ` haulla ”${query}”` : " käytettävissä"}
            </p>
            {resource.data.items.length === 0 ? (
              <Empty title="Liikettä ei löytynyt">
                <p>
                  Kokeile toista nimeä tai lisää oma liike liikekirjastossa.
                </p>
              </Empty>
            ) : (
              <div className="exercise-grid">
                {resource.data.items.map((exercise) => (
                  <article className="exercise-tile" key={exercise.id}>
                    <div className="tile-top">
                      <span className="exercise-icon">
                        <img
                          src="/logo-symbol.svg"
                          width="24"
                          height="24"
                          alt=""
                        />
                      </span>
                      <span
                        className={`badge ${exercise.custom ? "accent-badge" : ""}`}
                      >
                        {exercise.custom ? "Oma liike" : "Perusliike"}
                      </span>
                    </div>
                    <h3>{exercise.name}</h3>
                    <p>
                      {[exercise.primaryMuscleGroup, exercise.equipment]
                        .filter(Boolean)
                        .join(" · ") || "Ei lisätietoja"}
                    </p>
                    {exercise.notes && (
                      <p className="tile-notes">{exercise.notes}</p>
                    )}
                    {onSelect ? (
                      <button
                        className="button secondary"
                        disabled={action.busy}
                        onClick={() =>
                          void action.run(() => onSelect(exercise))
                        }
                      >
                        Lisää harjoitukseen
                        <Icon name="plus" size={18} />
                      </button>
                    ) : exercise.custom && !readOnly ? (
                      <button
                        className="text-button"
                        onClick={() => setForm(exercise)}
                      >
                        Muokkaa liikettä
                        <Icon name="arrow" size={17} />
                      </button>
                    ) : (
                      <span className="read-only">Valmis katalogiliike</span>
                    )}
                  </article>
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
      {action.error && <ErrorMessage message={action.error} />}
    </section>
  );
}
