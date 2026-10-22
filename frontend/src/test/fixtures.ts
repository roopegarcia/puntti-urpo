import type { Feedback, Workout, WorkoutSet } from "../types";
export const draftSet: WorkoutSet = {
  id: 12,
  setOrder: 0,
  weightKg: null,
  repetitions: null,
  rpe: null,
  setType: "WORKING",
  completed: false,
  estimates: {
    estimatedRir: null,
    repetitionBasedE1rmKg: null,
    e1rmStatus: "NOT_COMPLETED",
    warnings: [],
  },
};
export const workout: Workout = {
  id: 1,
  date: "2026-10-22",
  title: "Testiharjoitus",
  notes: null,
  startedAt: "2026-10-22T08:22:33.123Z",
  endedAt: null,
  exercises: [
    {
      id: 4,
      displayOrder: 0,
      notes: null,
      exercise: {
        id: 1,
        name: "Penkkipunnerrus",
        equipment: "Levytanko",
        primaryMuscleGroup: "Rinta",
        category: "Moninivelliike",
        notes: null,
        custom: false,
      },
      sets: [draftSet],
    },
  ],
};
export const quietFeedback: Feedback = {
  onDirty: () => {},
  onBusy: () => {},
  onNotice: () => {},
};
export const jsonResponse = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
