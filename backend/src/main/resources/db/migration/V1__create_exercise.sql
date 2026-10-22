CREATE TABLE exercise (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(100) NOT NULL,
    category varchar(50),
    primary_muscle_group varchar(50),
    equipment varchar(50),
    notes varchar(2000),
    custom boolean NOT NULL DEFAULT true,
    CONSTRAINT exercise_name_not_blank CHECK (length(btrim(name)) > 0),
    CONSTRAINT exercise_name_trimmed CHECK (name = btrim(name))
);

CREATE UNIQUE INDEX exercise_name_unique ON exercise (lower(name));
