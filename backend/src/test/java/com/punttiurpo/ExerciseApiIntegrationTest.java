package com.punttiurpo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExerciseApiIntegrationTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        AuthenticatedClient.properties(registry);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private AuthenticatedClient authenticated;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void clearCustomExercises() {
        jdbc.update("DELETE FROM exercise WHERE custom = true");
    }

    @Test
    void listsSeedsWithStablePaginationAndCaseInsensitiveLiteralSearch() throws Exception {
        JsonNode catalogue = body(send("GET", "/api/exercises?size=3", null), 200);
        assertThat(catalogue.path("items").size()).isEqualTo(3);
        assertThat(catalogue.path("totalElements").asInt()).isEqualTo(10);
        assertThat(catalogue.path("totalPages").asInt()).isEqualTo(4);
        JsonNode result = body(send("GET", "/api/exercises?q=PENKKI", null), 200);
        assertThat(result.path("items").get(0).path("name").asText()).isEqualTo("Penkkipunnerrus");
        assertThat(body(send("GET", "/api/exercises?q=%25", null), 200)
                .path("totalElements").asInt()).isZero();
        assertThat(body(send("GET", "/api/exercises?page=99", null), 200).path("items").isEmpty()).isTrue();
        long first = catalogue.path("items").get(0).path("id").asLong();
        long next = body(send("GET", "/api/exercises?page=1&size=3", null), 200)
                .path("items").get(0).path("id").asLong();
        assertThat(next).isNotEqualTo(first);
    }

    @Test
    void createsReadsAndReplacesACustomExercise() throws Exception {
        var created = send("POST", "/api/exercises", """
                {"name":"  Oma soutu  ","category":" Selkäliike ","notes":" Alku "}
                """);
        JsonNode exercise = body(created, 201);
        assertThat(exercise.path("custom").asBoolean()).isTrue();
        assertThat(exercise.path("name").asText()).isEqualTo("Oma soutu");
        String location = created.headers().firstValue("Location").orElseThrow();
        assertThat(body(send("GET", location, null), 200).path("notes").asText()).isEqualTo("Alku");
        JsonNode updated = body(send("PUT", location, "{\"name\":\"Uusi soutu\"}"), 200);
        assertThat(updated.path("id").asLong()).isEqualTo(exercise.path("id").asLong());
        assertThat(updated.path("category").isNull()).isTrue();
        assertThat(updated.path("notes").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT name FROM exercise WHERE id = ?", String.class,
                exercise.path("id").asLong())).isEqualTo("Uusi soutu");
    }

    @Test
    void rejectsDuplicateNamesAndRollsBackConflictingUpdate() throws Exception {
        body(send("POST", "/api/exercises", "{\"name\":\"penkkipunnerrus\"}"), 409);
        var created = send("POST", "/api/exercises", "{\"name\":\"Oma liike\"}");
        body(created, 201);
        String location = created.headers().firstValue("Location").orElseThrow();
        body(send("PUT", location, "{\"name\":\"PENKKIPUNNERRUS\"}"), 409);
        assertThat(body(send("GET", location, null), 200).path("name").asText()).isEqualTo("Oma liike");
    }

    @Test
    void protectsBuiltInExercisesAndDoesNotOfferDeletion() throws Exception {
        long id = jdbc.queryForObject("SELECT id FROM exercise WHERE name = 'Penkkipunnerrus'", Long.class);
        body(send("PUT", "/api/exercises/" + id, "{\"name\":\"Muutos\"}"), 403);
        body(send("DELETE", "/api/exercises/" + id, null), 405);
        assertThat(body(send("GET", "/api/exercises/" + id, null), 200).path("custom").asBoolean()).isFalse();
    }

    @Test
    void reportsValidationMalformedRequestsAndMissingRecordsWithoutInternalDetails() throws Exception {
        JsonNode error = body(send("POST", "/api/exercises", "{\"name\":\" \"}"), 400);
        assertThat(error.path("errors").get(0).path("field").asText()).isEqualTo("name");
        body(send("POST", "/api/exercises", "{\"name\":\"" + "a".repeat(101) + "\"}"), 400);
        body(send("POST", "/api/exercises", "{"), 400);
        body(send("POST", "/api/exercises", "null"), 400);
        body(send("GET", "/api/exercises/not-a-number", null), 400);
        body(send("GET", "/api/exercises/0", null), 400);
        body(send("GET", "/api/exercises/999999", null), 404);
        body(send("PUT", "/api/exercises/999999", "{\"name\":\"Puuttuva\"}"), 404);
    }

    @Test
    void rejectsInvalidPaginationAndOversizedQueries() throws Exception {
        for (String query : new String[]{"page=-1", "page=2147483647", "size=0", "size=101", "page=nope", "q=" + "a".repeat(101)}) {
            body(send("GET", "/api/exercises?" + query, null), 400);
        }
    }

    @Test
    void databaseEnforcesNameIntegrityWithoutRelyingOnHttpValidation() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO exercise (name) VALUES ('  ')") )
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO exercise (name) VALUES ('PENKKIPUNNERRUS')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void exposesCompatibleOpenApiAndSwaggerUi() throws Exception {
        JsonNode spec = body(send("GET", "/v3/api-docs", null), 200);
        assertThat(spec.path("paths").has("/api/exercises")).isTrue();
        assertThat(spec.path("paths").path("/api/exercises").path("post").path("responses").has("201")).isTrue();
        assertThat(spec.path("paths").path("/api/exercises/{id}").has("put")).isTrue();
        assertThat(send("GET", "/swagger-ui/index.html", null).statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> send(String method, String path, String payload) throws Exception {
        if (authenticated == null) authenticated = new AuthenticatedClient(port, "owner");
        return authenticated.send(method, path, payload);
    }

    private JsonNode body(HttpResponse<String> response, int expectedStatus) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(expectedStatus);
        JsonNode body = json.readTree(response.body());
        if (expectedStatus >= 400) {
            assertThat(response.headers().firstValue("Content-Type").orElse(""))
                    .startsWith("application/problem+json");
            assertThat(body.path("status").asInt()).isEqualTo(expectedStatus);
            assertThat(response.body()).doesNotContain("trace", "SQLException", "org.hibernate", "constraint [");
        }
        return body;
    }
}
