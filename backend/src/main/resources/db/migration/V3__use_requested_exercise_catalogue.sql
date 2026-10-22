-- Preserve IDs for the four equivalent exercises; custom exercises are untouched.
UPDATE exercise SET name = CASE name
    WHEN 'Takakyykky' THEN 'Barbell Back Squat'
    WHEN 'Maastaveto' THEN 'Deadlift'
    WHEN 'Penkkipunnerrus' THEN 'Bench Press'
    WHEN 'Hauiskääntö' THEN 'Dumbbell Curl'
END
WHERE custom = false AND name IN ('Takakyykky', 'Maastaveto', 'Penkkipunnerrus', 'Hauiskääntö');

-- These former catalogue entries have no workout references: the workout schema is V4.
DELETE FROM exercise WHERE custom = false
    AND name IN ('Pystypunnerrus', 'Leuanveto', 'Kulmasoutu', 'Jalkaprässi');

INSERT INTO exercise (name, category, primary_muscle_group, equipment, custom) VALUES
    ('Romanian Deadlift', 'Moninivelliike', 'Takaketju', NULL, false),
    ('Bulgarian Split Squat', 'Moninivelliike', 'Etureidet', NULL, false),
    ('Skull Crusher', 'Eristävä liike', 'Ojentajat', NULL, false),
    ('Dumbbell Lateral Raise', 'Eristävä liike', 'Olkapäät', 'Käsipainot', false),
    ('Cable Flys', 'Eristävä liike', 'Rinta', 'Talja', false),
    ('Cable Tricep. Extension', 'Eristävä liike', 'Ojentajat', 'Talja', false);
