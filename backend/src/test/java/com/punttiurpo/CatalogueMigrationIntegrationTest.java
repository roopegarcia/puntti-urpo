package com.punttiurpo;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.flywaydb.core.api.FlywayException;

@Testcontainers
class CatalogueMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6");

    @Test
    void upgradesExistingCatalogueWithoutChangingCustomExercisesOrEquivalentIds() {
        var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var jdbc = new JdbcTemplate(source);
        Flyway.configure().dataSource(source).target("2").load().migrate();
        Long benchId = jdbc.queryForObject("SELECT id FROM exercise WHERE name='Penkkipunnerrus'", Long.class);
        jdbc.update("INSERT INTO exercise (name, notes, custom) VALUES ('My existing custom', 'Keep this note', true)");
        Long customId = jdbc.queryForObject("SELECT id FROM exercise WHERE custom", Long.class);

        Flyway.configure().dataSource(source).target("4").load().migrate();
        Long workoutId = jdbc.queryForObject("INSERT INTO workout (date, title) VALUES ('2026-10-22', 'Keep workout') RETURNING id", Long.class);
        Long entryId = jdbc.queryForObject("INSERT INTO workout_exercise (workout_id, exercise_id, display_order) VALUES (?, ?, 0) RETURNING id", Long.class, workoutId, benchId);
        jdbc.update("INSERT INTO workout_set (workout_exercise_id, set_order, weight_kg, repetitions, rpe, set_type, completed) VALUES (?, 0, 82.25, 6, 8.5, 'WORKING', true)", entryId);

        var flyway = Flyway.configure().dataSource(source).load();
        flyway.migrate();
        assertThat(jdbc.queryForObject("SELECT id FROM exercise WHERE name='Penkkipunnerrus'", Long.class)).isEqualTo(benchId);
        assertThat(jdbc.queryForObject("SELECT notes FROM exercise WHERE id=? AND custom", String.class, customId))
                .isEqualTo("Keep this note");
        assertThat(jdbc.queryForList("SELECT name FROM exercise WHERE NOT custom", String.class))
                .containsExactlyInAnyOrder("Takakyykky", "Romanialainen maastaveto", "Bulgarialainen askelkyykky", "Maastaveto",
                        "Penkkipunnerrus", "Hauiskääntö käsipainoilla", "Ranskalainen punnerrus", "Vipunosto sivulle käsipainoilla", "Ristikkäistalja", "Ojentajapunnerrus taljassa");
        assertThat(jdbc.queryForObject("SELECT exercise_id FROM workout_exercise WHERE id=?", Long.class, entryId)).isEqualTo(benchId);
        assertThat(jdbc.queryForObject("SELECT title FROM workout WHERE id=?", String.class, workoutId)).isEqualTo("Keep workout");
        assertThat(jdbc.queryForObject("SELECT weight_kg FROM workout_set WHERE workout_exercise_id=?", java.math.BigDecimal.class, entryId)).isEqualByComparingTo("82.25");
        assertThat(jdbc.queryForObject("SELECT repetitions FROM workout_set WHERE workout_exercise_id=?", Integer.class, entryId)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT rpe FROM workout_set WHERE workout_exercise_id=?", java.math.BigDecimal.class, entryId)).isEqualByComparingTo("8.5");
        flyway.migrate();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM exercise", Integer.class)).isEqualTo(11);
        assertThat(flyway.info().applied()).hasSize(6);
        assertThat(jdbc.queryForObject("SELECT owner_key FROM workout WHERE id=?", String.class, workoutId)).isEqualTo("owner");
        assertThat(jdbc.queryForObject("SELECT owner_key FROM exercise WHERE id=?", String.class, customId)).isEqualTo("owner");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout WHERE owner_key='demo'", Integer.class)).isEqualTo(1);
    }

    @Test
    void conflictingCustomFinnishNameRollsBackWithoutChangingAnyCatalogueRows() {
        var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var jdbc = new JdbcTemplate(source);
        Flyway.configure().dataSource(source).schemas("collision").target("4").load().migrate();
        jdbc.update("INSERT INTO collision.exercise (name, notes, custom) VALUES ('Penkkipunnerrus', 'Keep custom', true)");
        var before = jdbc.queryForList("SELECT * FROM collision.exercise ORDER BY id");
        assertThatThrownBy(() -> Flyway.configure().dataSource(source).schemas("collision").load().migrate())
                .isInstanceOf(FlywayException.class);
        assertThat(jdbc.queryForList("SELECT * FROM collision.exercise ORDER BY id")).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM collision.flyway_schema_history WHERE version='5' AND success", Integer.class)).isZero();
    }

}
