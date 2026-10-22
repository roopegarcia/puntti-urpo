CREATE TABLE workout (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    date date NOT NULL,
    title varchar(100),
    notes varchar(2000),
    started_at timestamp with time zone,
    ended_at timestamp with time zone,
    CONSTRAINT workout_time_range CHECK (
        ended_at IS NULL OR (started_at IS NOT NULL AND ended_at >= started_at)
    )
);
CREATE INDEX workout_history_order ON workout (date DESC, id DESC);

CREATE TABLE workout_exercise (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workout_id bigint NOT NULL REFERENCES workout(id) ON DELETE CASCADE,
    exercise_id bigint NOT NULL REFERENCES exercise(id) ON DELETE RESTRICT,
    display_order integer NOT NULL CHECK (display_order BETWEEN 0 AND 1000000),
    notes varchar(2000),
    CONSTRAINT workout_exercise_order_unique UNIQUE (workout_id, display_order)
);
CREATE INDEX workout_exercise_catalogue_reference ON workout_exercise (exercise_id);

CREATE TABLE workout_set (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workout_exercise_id bigint NOT NULL REFERENCES workout_exercise(id) ON DELETE CASCADE,
    set_order integer NOT NULL CHECK (set_order BETWEEN 0 AND 1000000),
    weight_kg numeric(6,2) CHECK (weight_kg >= 0),
    repetitions integer CHECK (repetitions BETWEEN 0 AND 1000),
    rpe numeric(3,1) CHECK (rpe BETWEEN 6.0 AND 10.0 AND mod(rpe, 0.5) = 0),
    set_type varchar(10) NOT NULL CHECK (set_type IN ('WARMUP', 'WORKING')),
    completed boolean NOT NULL DEFAULT false,
    CONSTRAINT workout_set_order_unique UNIQUE (workout_exercise_id, set_order),
    CONSTRAINT completed_set_has_performance CHECK (
        NOT completed OR (weight_kg IS NOT NULL AND repetitions IS NOT NULL AND repetitions > 0)
    )
);
