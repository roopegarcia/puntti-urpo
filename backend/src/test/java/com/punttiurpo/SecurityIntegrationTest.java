package com.punttiurpo;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {
    @Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        AuthenticatedClient.properties(registry);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void anonymousUsersCannotReadPrivateApisOrSwaggerAndCannotWriteWithoutCsrf() throws Exception {
        var http = HttpClient.newHttpClient();
        for (String path : new String[]{"/api/workouts", "/api/exercises", "/api/sets/1", "/api/auth/me", "/v3/api-docs", "/swagger-ui/index.html"}) {
            var response = http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).doesNotContain("password", "trace", "owner_key");
        }
        var health = http.send(HttpRequest.newBuilder(uri("/actuator/health")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(health.statusCode()).isEqualTo(200);
        var blocked = http.send(HttpRequest.newBuilder(uri("/api/workouts")).POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(blocked.statusCode()).isEqualTo(403);
    }

    @Test
    void eachAccountSeesOnlyItsRecordsIncludingNestedIdsAndCustomCatalogue() throws Exception {
        var owner = new AuthenticatedClient(port, "owner");
        var demo = new AuthenticatedClient(port, "demo");
        var created = owner.send("POST", "/api/workouts", "{\"date\":\"2026-10-22\",\"title\":\"Private workout\",\"ownerKey\":\"demo\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        long workout = json.readTree(created.body()).path("id").asLong();
        assertThat(jdbc.queryForObject("SELECT owner_key FROM workout WHERE id=?", String.class, workout)).isEqualTo("owner");
        long exercise = jdbc.queryForObject("SELECT id FROM exercise WHERE name='Penkkipunnerrus'", Long.class);
        var entryResponse = owner.send("POST", "/api/workouts/" + workout + "/exercises", "{\"exerciseId\":" + exercise + "}");
        assertThat(entryResponse.statusCode()).isEqualTo(201);
        long entry = json.readTree(entryResponse.body()).path("id").asLong();
        var setResponse = owner.send("POST", "/api/workout-exercises/" + entry + "/sets", "{\"weightKg\":72.5,\"repetitions\":6,\"completed\":true}");
        assertThat(setResponse.statusCode()).isEqualTo(201);
        long set = json.readTree(setResponse.body()).path("id").asLong();
        for (String path : new String[]{"/api/workouts/" + workout, "/api/workout-exercises/" + entry, "/api/sets/" + set}) {
            assertThat(demo.send("GET", path, null).statusCode()).isEqualTo(404);
        }
        var custom = owner.send("POST", "/api/exercises", "{\"name\":\"Oma salainen variaatio\",\"notes\":\"Private note\"}");
        assertThat(custom.statusCode()).isEqualTo(201);
        long customId = json.readTree(custom.body()).path("id").asLong();
        assertThat(demo.send("GET", "/api/exercises/" + customId, null).statusCode()).isEqualTo(404);
        assertThat(json.readTree(demo.send("GET", "/api/exercises?q=salainen", null).body()).path("totalElements").asInt()).isZero();
        assertThat(demo.send("GET", "/api/workouts", null).body()).doesNotContain("Private workout", "Private note");
        long demoWorkout = jdbc.queryForObject("SELECT id FROM workout WHERE owner_key='demo'", Long.class);
        long demoEntry = jdbc.queryForObject("SELECT id FROM workout_exercise WHERE workout_id=?", Long.class, demoWorkout);
        long demoSet = jdbc.queryForObject("SELECT id FROM workout_set WHERE workout_exercise_id=?", Long.class, demoEntry);
        for (String path : new String[]{"/api/workouts/" + demoWorkout, "/api/workout-exercises/" + demoEntry, "/api/sets/" + demoSet}) {
            assertThat(owner.send("GET", path, null).statusCode()).isEqualTo(404);
            String payload = path.contains("/workout-exercises/") ? "{\"displayOrder\":0}"
                    : path.contains("/sets/") ? "{}" : "{\"date\":\"2026-10-22\"}";
            assertThat(owner.send("PUT", path, payload).statusCode()).isEqualTo(404);
            assertThat(owner.send("DELETE", path, null).statusCode()).isEqualTo(404);
        }
        assertThat(owner.send("GET", "/api/workouts", null).body()).doesNotContain("Esimerkkiharjoitus");
        assertThat(demo.send("GET", "/api/workouts/" + demoWorkout, null).statusCode()).isEqualTo(200);
    }

    @Test
    void demoCannotMutateAnyDomainEndpointEvenWithValidCsrf() throws Exception {
        var demo = new AuthenticatedClient(port, "demo");
        for (String path : new String[]{"/api/workouts", "/api/workouts/1", "/api/workouts/1/exercises", "/api/workout-exercises/1", "/api/workout-exercises/1/sets", "/api/sets/1", "/api/exercises", "/api/exercises/1"}) {
            for (String method : new String[]{"POST", "PUT", "DELETE", "PATCH"}) {
                assertThat(demo.send(method, path, "{}").statusCode()).isEqualTo(403);
            }
        }
        assertThat(demo.send("GET", "/v3/api-docs", null).statusCode()).isEqualTo(403);
    }

    @Test
    void loginRotatesSessionCsrfIsRequiredAndLogoutInvalidatesIt() throws Exception {
        var cookies = new CookieManager();
        var http = HttpClient.newBuilder().cookieHandler(cookies).build();
        var csrfResponse = http.send(HttpRequest.newBuilder(uri("/api/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        String csrf = json.readTree(csrfResponse.body()).path("token").asText();
        String before = cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("PUNTTI_SESSION")).findFirst().orElseThrow().getValue();
        assertThat(csrfResponse.headers().allValues("Set-Cookie").toString()).contains("HttpOnly", "SameSite=Strict");
        var noCsrf = http.send(HttpRequest.newBuilder(uri("/api/auth/login")).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=owner&password=" + AuthenticatedClient.PASSWORD)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(noCsrf.statusCode()).isEqualTo(403);
        var wrong = http.send(HttpRequest.newBuilder(uri("/api/auth/login")).header("Content-Type", "application/x-www-form-urlencoded").header("X-CSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString("username=owner&password=wrong")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(wrong.statusCode()).isEqualTo(401);
        var login = http.send(HttpRequest.newBuilder(uri("/api/auth/login")).header("Content-Type", "application/x-www-form-urlencoded").header("X-CSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString("username=owner&password=" + AuthenticatedClient.PASSWORD)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(204);
        String after = cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("PUNTTI_SESSION")).findFirst().orElseThrow().getValue();
        assertThat(after).isNotEqualTo(before);
        assertThat(http.send(HttpRequest.newBuilder(uri("/api/workouts")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        var noToken = http.send(HttpRequest.newBuilder(uri("/api/workouts")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(noToken.statusCode()).isEqualTo(403);
        var newToken = http.send(HttpRequest.newBuilder(uri("/api/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        String fresh = json.readTree(newToken.body()).path("token").asText();
        assertThat(http.send(HttpRequest.newBuilder(uri("/api/auth/logout")).header("X-CSRF-TOKEN", fresh).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(204);
        assertThat(http.send(HttpRequest.newBuilder(uri("/api/workouts")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
}
