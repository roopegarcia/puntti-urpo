package com.punttiurpo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FoundationIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6")
            .withUrlParam("connectTimeout", "2")
            .withUrlParam("socketTimeout", "2");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Flyway flyway;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void connectsToRealPostgresql() {
        assertThat(jdbc.queryForObject("SELECT version()", String.class))
                .startsWith("PostgreSQL 18.6");
    }

    @Test
    void appliesCatalogueMigrationsAndValidatesSchema() {
        assertThat(flyway.info().applied()).hasSize(4);
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(jdbc.queryForList("""
                SELECT tablename FROM pg_tables
                WHERE schemaname = 'public'
                """, String.class)).containsExactlyInAnyOrder("flyway_schema_history", "exercise", "workout", "workout_exercise", "workout_set");
    }

    @Test
    void reportsHealthDuringDatabaseOutageAndRecoveryWithoutExposingDetails() throws Exception {
        assertHealth(200, "{\"status\":\"UP\"}");

        // Pause preserves the container's mapped port and data while JDBC stops responding.
        postgres.getDockerClient().pauseContainerCmd(postgres.getContainerId()).exec();
        try {
            assertHealth(503, "{\"status\":\"DOWN\"}");
        } finally {
            postgres.getDockerClient().unpauseContainerCmd(postgres.getContainerId()).exec();
        }

        assertHealth(200, "{\"status\":\"UP\"}");
    }

    private void assertHealth(int statusCode, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(statusCode);
        assertThat(response.body()).isEqualTo(body);
    }
}
