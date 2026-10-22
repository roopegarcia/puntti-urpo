# Database

## Implemented state

PostgreSQL 18.6 runs through `compose.yaml` in the dedicated `puntti-urpo` Compose project. It has its own network and `puntti-urpo_postgres_data` named volume. Host access binds only to `127.0.0.1`, default port 54328. The backend uses JDBC and HikariCP, not JPA yet.

The PostgreSQL 18 official image stores version-specific data underneath `/var/lib/postgresql`; mount the volume at that parent path, as this Compose file does. The image initializes `POSTGRES_DB`, `POSTGRES_USER` and `POSTGRES_PASSWORD` only for a new data directory. Changing `.env` later does not automatically change stored users, passwords or database names.

## Configuration and migrations

The backend reads `DB_HOST`, `DB_PORT`, `POSTGRES_DB`, `POSTGRES_USER` and required `POSTGRES_PASSWORD` from its environment. Compose loads `.env`; export it separately before starting Java, as documented in the README. No real credentials belong in tracked files.

Flyway scans `backend/src/main/resources/db/migration/`. The README there is explanatory text, not a SQL migration. There are deliberately no application migrations in Milestone 1. Startup creates `flyway_schema_history`; integration tests verify zero applied/pending migrations and no domain tables. Flyway may warn that no migrations were found; that is expected for this milestone.

Migration validation is enabled and Flyway clean is disabled. Milestone 2 introduces the first domain SQL migration. Applied migrations are history: add a new migration to change an existing shared schema rather than editing an already applied migration. Hibernate must eventually validate this schema instead of changing it.

## Inspecting the local database

From the repository root with the database running:

```sh
docker compose exec db sh -c 'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Inside psql, use `\dt`, `SELECT version();` and `\q`. Do not paste credentials into commands or logs.

## Persistence and isolation

`docker compose stop db`, `docker compose restart db` and `docker compose down` preserve the named volume. `docker compose down -v` deletes project data and is not a routine restart command. The milestone verification created a disposable marker, checked it after stop/start, then removed its entire verification schema.

Tests use disposable Testcontainers databases with generated connection settings and random ports. They do not connect to the Compose database or reuse its named volume. Failure to access Docker fails the tests; it does not silently replace PostgreSQL with an in-memory database.

## Domain modeling: deferred

There are no domain entities, foreign keys, application indexes, JPA mappings or business transaction boundaries yet. No implemented ER diagram can be shown at this stage. Add the actual ER diagram as tables arrive, rather than presenting a proposal as a live schema.

Exercise, Workout, WorkoutExercise and WorkoutSet remain planned concepts. Before their implementation, define IDs, relationships, foreign keys, indexes, decimal precision, ordering, constraints and deletion behavior. Document primary/foreign keys, relational integrity and transaction behavior with the actual SQL and Java code at that milestone.
