import type { SetInput, SetType, WorkoutSet, WorkoutSummary } from "./types";
export const nullable = (value: string) => value.trim() || null;
export const numberText = (value: number | null) =>
  value === null ? "" : String(value).replace(".", ",");
export const formatNumber = (value: number, maximumFractionDigits = 2) =>
  new Intl.NumberFormat("fi-FI", { maximumFractionDigits }).format(value);
export const formatDate = (date: string) =>
  new Intl.DateTimeFormat("fi-FI", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${date}T12:00:00`));
export function todayLocal(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
export function localDateTime(value: string | null) {
  if (!value) return "";
  const date = new Date(value);
  return `${todayLocal(date)}T${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}
export function instant(value: string) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) throw new Error("Tarkista kellonaika.");
  return date.toISOString();
}
export interface SetDraft {
  weight: string;
  reps: string;
  rpe: string;
  type: SetType;
}
export function draftFrom(set: WorkoutSet): SetDraft {
  return {
    weight: numberText(set.weightKg),
    reps: numberText(set.repetitions),
    rpe: set.rpe === null ? "" : String(set.rpe),
    type: set.setType,
  };
}
export function setPayload(draft: SetDraft, completed: boolean): SetInput {
  const weight = draft.weight.trim().replace(",", ".");
  const reps = draft.reps.trim();
  if (
    weight &&
    (!/^(?:\d+(?:\.\d{1,2})?|\.\d{1,2})$/.test(weight) ||
      Number(weight) > 9999.99)
  )
    throw new Error(
      "Anna paino välillä 0–9999,99 kg, enintään kahdella desimaalilla.",
    );
  if (reps && (!/^\d+$/.test(reps) || Number(reps) > 1000))
    throw new Error("Anna kokonainen toistomäärä välillä 0–1000.");
  const weightKg = weight ? Number(weight) : null;
  const repetitions = reps ? Number(reps) : null;
  if (
    completed &&
    (weightKg === null || repetitions === null || repetitions < 1)
  )
    throw new Error(
      "Valmis sarja tarvitsee painon ja vähintään yhden toiston.",
    );
  const rpe = draft.rpe ? Number(draft.rpe) : null;
  if (
    rpe !== null &&
    (!Number.isFinite(rpe) || rpe < 6 || rpe > 10 || (rpe * 2) % 1 !== 0)
  )
    throw new Error("Anna RPE välillä 6–10 puolen pisteen välein.");
  // Convert input text to JSON numbers only; never calculate strength estimates in JS.
  return { weightKg, repetitions, rpe, setType: draft.type, completed };
}
export function nextSet(sets: WorkoutSet[]): SetInput {
  const previous = sets.filter((s) => s.completed).at(-1) ?? sets.at(-1);
  return {
    weightKg: previous?.weightKg ?? null,
    repetitions: previous?.repetitions ?? null,
    rpe: null,
    setType: previous?.setType ?? "WORKING",
    completed: false,
  };
}
export function workoutPayload(workout: WorkoutSummary) {
  return {
    date: workout.date,
    title: workout.title,
    notes: workout.notes,
    startedAt: workout.startedAt,
    endedAt: workout.endedAt,
  };
}
