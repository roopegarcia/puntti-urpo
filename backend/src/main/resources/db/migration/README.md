# Database migrations

- V1 creates the exercise table and name constraints.
- V2 records the original eight Finnish catalogue entries.
- V3 changes built-ins to the ten user-requested English exercise names, preserving
  equivalent IDs and custom rows. A name collision fails safely; do not delete a
  custom record to force the migration through without discussing resolution.
- V4 creates workouts, logged exercises and sets, with exact numeric types,
  ordered-parent uniqueness, performance checks and explicit foreign-key deletes.

Never edit applied migrations. Add a new migration for changes; Hibernate validates
rather than generates schema. Fresh and existing databases follow the same history.
