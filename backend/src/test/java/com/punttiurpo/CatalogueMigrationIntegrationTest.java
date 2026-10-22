package com.punttiurpo;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;

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

        var flyway = Flyway.configure().dataSource(source).load();
        flyway.migrate();
        assertThat(jdbc.queryForObject("SELECT id FROM exercise WHERE name='Bench Press'", Long.class)).isEqualTo(benchId);
        assertThat(jdbc.queryForObject("SELECT notes FROM exercise WHERE id=? AND custom", String.class, customId))
                .isEqualTo("Keep this note");
        assertThat(jdbc.queryForList("SELECT name FROM exercise WHERE NOT custom", String.class))
                .containsExactlyInAnyOrder("Barbell Back Squat", "Romanian Deadlift", "Bulgarian Split Squat", "Deadlift",
                        "Bench Press", "Dumbbell Curl", "Skull Crusher", "Dumbbell Lateral Raise", "Cable Flys", "Cable Tricep. Extension");
        flyway.migrate();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM exercise", Integer.class)).isEqualTo(11);
        assertThat(flyway.info().applied()).hasSize(4);
    }
}
