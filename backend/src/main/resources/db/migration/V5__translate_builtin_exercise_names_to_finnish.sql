-- Rename built-ins in place: preserve catalogue IDs, workout references and custom rows.
-- A collision with an existing custom name fails atomically under the unique-name index.
UPDATE exercise SET name = CASE name
    WHEN 'Barbell Back Squat' THEN 'Takakyykky'
    WHEN 'Romanian Deadlift' THEN 'Romanialainen maastaveto'
    WHEN 'Bulgarian Split Squat' THEN 'Bulgarialainen askelkyykky'
    WHEN 'Deadlift' THEN 'Maastaveto'
    WHEN 'Bench Press' THEN 'Penkkipunnerrus'
    WHEN 'Dumbbell Curl' THEN 'Hauiskääntö käsipainoilla'
    WHEN 'Skull Crusher' THEN 'Ranskalainen punnerrus'
    WHEN 'Dumbbell Lateral Raise' THEN 'Vipunosto sivulle käsipainoilla'
    WHEN 'Cable Flys' THEN 'Ristikkäistalja'
    WHEN 'Cable Tricep. Extension' THEN 'Ojentajapunnerrus taljassa'
END
WHERE custom = false AND name IN (
    'Barbell Back Squat', 'Romanian Deadlift', 'Bulgarian Split Squat', 'Deadlift',
    'Bench Press', 'Dumbbell Curl', 'Skull Crusher', 'Dumbbell Lateral Raise',
    'Cable Flys', 'Cable Tricep. Extension'
);
