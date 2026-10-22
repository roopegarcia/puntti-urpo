# Database migrations

Flyway is configured to read versioned SQL migrations from this directory.
There are deliberately no SQL migrations in Milestone 1. Flyway creates its
own schema-history table, but no application tables. The first domain migration
belongs to the exercise catalogue in Milestone 2.

Use names such as `V1__create_exercise.sql` when that milestone is approved.
Never edit a migration that has already been applied to a shared database.
