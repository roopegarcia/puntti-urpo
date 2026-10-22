import { useRef, useState } from "react";
import { api } from "../api";
import { useAction, useDirty } from "../hooks";
import type { Feedback, WorkoutSet } from "../types";
import { draftFrom, formatNumber, setPayload } from "../utils";
import { ConfirmDelete, Icon } from "./Shared";

export function SetRow({
  set,
  number,
  onUpdate,
  onDelete,
  feedback,
}: {
  set: WorkoutSet;
  number: number;
  onUpdate: (set: WorkoutSet) => void;
  onDelete: (id: number) => void;
  feedback: Feedback;
}) {
  const [draft, setDraft] = useState(() => draftFrom(set));
  const dirty = JSON.stringify(draft) !== JSON.stringify(draftFrom(set));
  const action = useAction(feedback);
  useDirty(dirty, feedback);
  const errorRef = useRef<HTMLParagraphElement>(null);
  async function save(completed: boolean) {
    await action.run(
      async () => {
        let body;
        try {
          body = setPayload(draft, completed);
        } catch (error) {
          requestAnimationFrame(() => errorRef.current?.focus());
          throw error;
        }
        const updated = await api<WorkoutSet>(`/api/sets/${set.id}`, {
          method: "PUT",
          body,
        });
        setDraft(draftFrom(updated));
        onUpdate(updated);
      },
      completed
        ? `Sarja ${number} tallennettu valmiiksi.`
        : `Sarja ${number} tallennettu keskeneräisenä.`,
    );
  }
  const estimates = set.estimates;
  return (
    <div className={`set-row ${set.completed ? "is-complete" : ""}`}>
      <div className="set-heading">
        <span className="set-number">{String(number).padStart(2, "0")}</span>
        <label className="type-label">
          <span className="sr-only">Sarjan {number} tyyppi</span>
          <select
            value={draft.type}
            disabled={action.busy}
            onChange={(e) =>
              setDraft({
                ...draft,
                type: e.target.value as "WORKING" | "WARMUP",
              })
            }
          >
            <option value="WORKING">Työsarja</option>
            <option value="WARMUP">Lämmittely</option>
          </select>
        </label>
        <span className="set-state">
          {dirty ? "Muokattu" : set.completed ? "Tallennettu" : "Luonnos"}
        </span>
        <ConfirmDelete
          label={`Poista sarja ${number}`}
          question={`Poistetaanko sarja ${number}?`}
          disabled={action.busy}
          onConfirm={() =>
            action.run(async () => {
              await api<void>(`/api/sets/${set.id}`, { method: "DELETE" });
              onDelete(set.id);
            }, "Sarja poistettu.")
          }
        />
      </div>
      <fieldset disabled={action.busy} className="set-fields">
        <legend className="sr-only">Sarja {number}</legend>
        <label htmlFor={`weight-${set.id}`}>
          <span>
            Paino <small>kg</small>
          </span>
          <input
            id={`weight-${set.id}`}
            inputMode="decimal"
            type="text"
            autoComplete="off"
            value={draft.weight}
            aria-describedby={action.error ? `set-error-${set.id}` : undefined}
            onChange={(e) => setDraft({ ...draft, weight: e.target.value })}
            placeholder="–"
          />
        </label>
        <label htmlFor={`reps-${set.id}`}>
          <span>Toistot</span>
          <input
            id={`reps-${set.id}`}
            inputMode="numeric"
            type="text"
            autoComplete="off"
            value={draft.reps}
            onChange={(e) => setDraft({ ...draft, reps: e.target.value })}
            placeholder="–"
          />
        </label>
        <label htmlFor={`rpe-${set.id}`}>
          <span>
            RPE <small>valinn.</small>
          </span>
          <select
            id={`rpe-${set.id}`}
            value={draft.rpe}
            onChange={(e) => setDraft({ ...draft, rpe: e.target.value })}
          >
            <option value="">–</option>
            {[6, 6.5, 7, 7.5, 8, 8.5, 9, 9.5, 10].map((value) => (
              <option key={value} value={value}>
                {formatNumber(value)}
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          className={`complete-button ${set.completed ? "checked" : ""}`}
          aria-label={
            set.completed
              ? `Merkitse sarja ${number} keskeneräiseksi`
              : `Merkitse sarja ${number} valmiiksi`
          }
          aria-pressed={set.completed}
          onClick={() => void save(!set.completed)}
        >
          <Icon name="check" />
          <span>
            {action.busy
              ? "Tallentuu…"
              : set.completed
                ? "Valmis"
                : "Valmiiksi"}
          </span>
        </button>
      </fieldset>
      {action.error && (
        <div id={`set-error-${set.id}`} role="alert" className="field-error">
          <p ref={errorRef} tabIndex={-1}>
            {action.error}
          </p>
        </div>
      )}
      {dirty && (
        <div className="draft-actions">
          <span>Muutoksia ei ole vielä tallennettu.</span>
          <button
            type="button"
            className="text-button"
            disabled={action.busy}
            onClick={() => void save(set.completed)}
          >
            Tallenna muutokset
          </button>
        </div>
      )}
      {set.completed && !dirty && estimates && (
        <div className="set-estimates">
          <span>
            RIR ≈{" "}
            <strong>
              {estimates.estimatedRir === null
                ? "ei arviota"
                : formatNumber(estimates.estimatedRir, 1)}
            </strong>
          </span>
          {estimates.repetitionBasedE1rmKg !== null ? (
            <span>
              Toistoperusteinen e1RM ≈{" "}
              <strong>
                {formatNumber(estimates.repetitionBasedE1rmKg)} kg
              </strong>
            </span>
          ) : (
            <span>
              {
                {
                  NOT_WORKING_SET: "Lämmittely: ei e1RM-arviota",
                  NO_POSITIVE_LOAD:
                    "Ei e1RM-arviota ilman positiivista kuormaa",
                  UNSUPPORTED_REPETITIONS: "e1RM vain 1–10 toistosta",
                  NOT_COMPLETED: "Keskeneräinen sarja",
                  AVAILABLE: "",
                }[estimates.e1rmStatus]
              }
            </span>
          )}
          {estimates.warnings.map((warning) => (
            <small className="estimate-warning" key={warning}>
              {
                {
                  SUBMAXIMAL_RPE_NOT_ADJUSTED:
                    "RPE alle 10: e1RM ei huomioi varastoon jääneitä toistoja.",
                  RPE_NOT_RECORDED:
                    "RPE puuttuu: sarjan maksimaalisesta rasittavuudesta ei ole tietoa.",
                  SINGLE_REPETITION_NOT_VERIFIED_MAXIMUM:
                    "Yhden toiston arvo on kirjattu kuorma, ei varmennettu maksimi.",
                }[warning]
              }
            </small>
          ))}
        </div>
      )}
    </div>
  );
}
