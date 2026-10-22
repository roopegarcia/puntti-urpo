export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface Exercise {
  id: number;
  name: string;
  category: string | null;
  primaryMuscleGroup: string | null;
  equipment: string | null;
  notes: string | null;
  custom: boolean;
}
export type ExerciseInput = Omit<Exercise, "id" | "custom">;
export interface Estimates {
  estimatedRir: number | null;
  repetitionBasedE1rmKg: number | null;
  e1rmStatus:
    | "AVAILABLE"
    | "NOT_COMPLETED"
    | "NOT_WORKING_SET"
    | "NO_POSITIVE_LOAD"
    | "UNSUPPORTED_REPETITIONS";
  warnings: (
    | "SUBMAXIMAL_RPE_NOT_ADJUSTED"
    | "RPE_NOT_RECORDED"
    | "SINGLE_REPETITION_NOT_VERIFIED_MAXIMUM"
  )[];
}
export type SetType = "WORKING" | "WARMUP";
export interface WorkoutSet {
  id: number;
  setOrder: number;
  weightKg: number | null;
  repetitions: number | null;
  rpe: number | null;
  setType: SetType;
  completed: boolean;
  estimates: Estimates;
}
export interface SetInput {
  setOrder?: number;
  weightKg: number | null;
  repetitions: number | null;
  rpe: number | null;
  setType: SetType;
  completed: boolean;
}
export interface LoggedExercise {
  id: number;
  exercise: Exercise;
  displayOrder: number;
  notes: string | null;
  sets: WorkoutSet[];
}
export interface WorkoutSummary {
  id: number;
  date: string;
  title: string | null;
  notes: string | null;
  startedAt: string | null;
  endedAt: string | null;
}
export type WorkoutInput = Omit<WorkoutSummary, "id">;
export interface Workout extends WorkoutSummary {
  exercises: LoggedExercise[];
}
export interface Feedback {
  onDirty: (key: string, dirty: boolean) => void;
  onBusy: (key: string, busy: boolean) => void;
  onNotice: (message: string) => void;
}
