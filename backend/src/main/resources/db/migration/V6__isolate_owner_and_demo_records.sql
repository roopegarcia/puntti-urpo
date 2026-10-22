-- Existing local records belong only to the owner. Authentication accounts are configured
-- outside the database; these two stable keys are the complete supported account registry.
ALTER TABLE workout ADD COLUMN owner_key varchar(10) NOT NULL DEFAULT 'owner'
    CHECK (owner_key IN ('owner', 'demo'));
CREATE INDEX workout_owner_history ON workout(owner_key, date DESC, id DESC);
ALTER TABLE exercise ADD COLUMN owner_key varchar(10);
UPDATE exercise SET owner_key = 'owner' WHERE custom;
ALTER TABLE exercise ADD CONSTRAINT exercise_owner_scope CHECK (
    (custom AND owner_key IS NOT NULL AND owner_key = 'owner') OR (NOT custom AND owner_key IS NULL)
);
-- The demo is intentionally read-only; no custom demo exercises are supported.
-- Global name uniqueness remains valid because only one owner may create custom rows.
ALTER TABLE exercise ALTER COLUMN owner_key SET DEFAULT 'owner';

-- Entirely invented example data, never copied from personal workout records.
INSERT INTO workout(date, title, notes, started_at, ended_at, owner_key)
VALUES ('2026-10-19', 'Esimerkkiharjoitus: ylävartalo', 'Kuvitteellinen demoharjoitus. Ei oikeita henkilötietoja.',
        '2026-10-19T14:00:00Z', '2026-10-19T14:45:00Z', 'demo');
INSERT INTO workout_exercise(workout_id, exercise_id, display_order)
SELECT w.id, e.id, 0 FROM workout w CROSS JOIN exercise e
WHERE w.owner_key = 'demo' AND e.name = 'Penkkipunnerrus' AND NOT e.custom;
INSERT INTO workout_set(workout_exercise_id, set_order, weight_kg, repetitions, rpe, set_type, completed)
SELECT we.id, 0, 60, 8, 8, 'WORKING', true FROM workout_exercise we
JOIN workout w ON w.id = we.workout_id WHERE w.owner_key = 'demo';
