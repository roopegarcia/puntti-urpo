package com.punttiurpo;

import java.math.BigDecimal;
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
class WorkoutApiIntegrationTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.6");

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
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void clearOnlyThisTestDatabaseWorkouts() {
        jdbc.update("DELETE FROM workout");
    }

    @Test
    void createsReadsUpdatesAndListsWorkoutMetadataWithoutReplacingChildren() throws Exception {
        String path = createWorkout("2026-10-22");
        long exercise = addExercise(path);
        body(send("POST", "/api/workout-exercises/" + exercise + "/sets", "{}"), 201);
        var changed = body(send("PUT", path, """
                {"date":"2026-10-23","title":"  Jalkapäivä  ","notes":" Muistiinpano ",
                 "startedAt":"2026-10-23T14:00:00Z","endedAt":"2026-10-23T15:00:00Z"}
                """), 200);
        assertThat(changed.path("title").asText()).isEqualTo("Jalkapäivä");
        assertThat(changed.path("exercises").get(0).path("sets").size()).isEqualTo(1);
        createWorkout("2026-10-21");
        var list = body(send("GET", "/api/workouts?size=1", null), 200);
        assertThat(list.path("totalElements").asInt()).isEqualTo(2);
        assertThat(list.path("totalPages").asInt()).isEqualTo(2);
        assertThat(list.path("items").get(0).path("date").asText()).isEqualTo("2026-10-23");
        assertThat(list.path("items").get(0).has("exercises")).isFalse();
    }

    @Test
    void draftSetCanBeCompletedAndExactDecimalValuesSurviveRoundTrip() throws Exception {
        String workout = createWorkout("2026-10-22");
        long exercise = addExercise(workout);
        var added = send("POST", "/api/workout-exercises/" + exercise + "/sets", "{}");
        var draft = body(added, 201);
        assertThat(draft.path("completed").asBoolean()).isFalse();
        assertThat(draft.path("weightKg").isNull()).isTrue();
        assertThat(draft.path("setType").asText()).isEqualTo("WORKING");
        String location = added.headers().firstValue("Location").orElseThrow();
        var completed = body(send("PUT", location, """
                {"weightKg":82.25,"repetitions":6,"rpe":8.5,"setType":"WARMUP","completed":true}
                """), 200);
        assertThat(completed.path("completed").asBoolean()).isTrue();
        assertThat(completed.path("setOrder").asInt()).isZero();
        var stored = body(send("GET", location, null), 200);
        assertThat(stored.path("weightKg").decimalValue()).isEqualByComparingTo("82.25");
        assertThat(stored.path("rpe").decimalValue()).isEqualByComparingTo("8.5");
        assertThat(jdbc.queryForObject("SELECT weight_kg FROM workout_set WHERE id = ?", BigDecimal.class,
                completed.path("id").asLong())).isEqualByComparingTo("82.25");
        assertThat(body(send("GET", workout, null), 200).path("exercises").get(0)
                .path("sets").get(0).path("id").asLong()).isEqualTo(completed.path("id").asLong());
    }

    @Test
    void enforcesSetBoundariesAndRejectsInvalidCompletedSets() throws Exception {
        String workout = createWorkout("2026-10-22");
        String sets = "/api/workout-exercises/" + addExercise(workout) + "/sets";
        for (String invalid : new String[]{
                "{\"completed\":true}",
                "{\"weightKg\":20,\"repetitions\":0,\"completed\":true}",
                "{\"weightKg\":-1}", "{\"weightKg\":20.001}", "{\"weightKg\":10000}",
                "{\"repetitions\":-1}", "{\"repetitions\":1001}",
                "{\"rpe\":5.5}", "{\"rpe\":10.5}", "{\"rpe\":8.2}", "{\"rpe\":8.55}",
                "{\"setType\":\"UNKNOWN\"}", "{\"setOrder\":-1}"}) {
            body(send("POST", sets, invalid), 400);
        }
        body(send("POST", sets, "{\"weightKg\":0,\"repetitions\":1,\"rpe\":6,\"completed\":true}"), 201);
        body(send("POST", sets, "{\"weightKg\":9999.99,\"repetitions\":1000,\"rpe\":10,\"completed\":true}"), 201);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_set", Integer.class)).isEqualTo(2);
    }

    @Test
    void preservesOrderingAndRollsBackDuplicatePositions() throws Exception {
        String workout = createWorkout("2026-10-22");
        long first = addExercise(workout);
        long second = addExercise(workout);
        body(send("PUT", "/api/workout-exercises/" + first, "{\"displayOrder\":1,\"notes\":\"Must roll back\"}"), 409);
        assertThat(body(send("GET", "/api/workout-exercises/" + first, null), 200).path("notes").isNull()).isTrue();
        body(send("PUT", "/api/workout-exercises/" + first, "{\"displayOrder\":5,\"notes\":\"Last\"}"), 200);
        var response = body(send("GET", workout, null), 200);
        assertThat(response.path("exercises").get(0).path("id").asLong()).isEqualTo(second);
        String setPath = "/api/workout-exercises/" + second + "/sets";
        var one = body(send("POST", setPath, "{}"), 201);
        var two = body(send("POST", setPath, "{}"), 201);
        assertThat(two.path("setOrder").asInt()).isEqualTo(1);
        body(send("PUT", "/api/sets/" + two.path("id").asLong(), "{\"setOrder\":0,\"weightKg\":99}"), 409);
        assertThat(body(send("GET", "/api/sets/" + two.path("id").asLong(), null), 200)
                .path("weightKg").isNull()).isTrue();
        body(send("PUT", "/api/sets/" + one.path("id").asLong(), "{\"setOrder\":4}"), 200);
        assertThat(body(send("GET", "/api/workout-exercises/" + second, null), 200)
                .path("sets").get(0).path("id").asLong()).isEqualTo(two.path("id").asLong());
    }

    @Test
    void deletesOnlyTheRequestedSetAndLoggedExercise() throws Exception {
        String workout = createWorkout("2026-10-22");
        long first = addExercise(workout);
        long second = addExercise(workout);
        long set = body(send("POST", "/api/workout-exercises/" + first + "/sets", "{}"), 201).path("id").asLong();
        long retainedSet = body(send("POST", "/api/workout-exercises/" + second + "/sets", "{}"), 201).path("id").asLong();
        assertThat(send("DELETE", "/api/sets/" + set, null).statusCode()).isEqualTo(204);
        body(send("GET", "/api/sets/" + set, null), 404);
        body(send("POST", "/api/workout-exercises/" + first + "/sets", "{}"), 201);
        assertThat(send("DELETE", "/api/workout-exercises/" + first, null).statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_set", Integer.class)).isEqualTo(1);
        body(send("GET", "/api/sets/" + retainedSet, null), 200);
        assertThat(body(send("GET", workout, null), 200).path("exercises").size()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM exercise WHERE custom=false", Integer.class)).isEqualTo(10);
    }

    @Test
    void workoutDeletionCascadesOnlyToItsChildrenAndCatalogueDeletionIsRestricted() throws Exception {
        String first = createWorkout("2026-10-22");
        String second = createWorkout("2026-10-22");
        long logged = addExercise(first);
        long retained = addExercise(second);
        body(send("POST", "/api/workout-exercises/" + logged + "/sets", "{}"), 201);
        long retainedSet = body(send("POST", "/api/workout-exercises/" + retained + "/sets", "{}"), 201).path("id").asLong();
        long catalogueId = jdbc.queryForObject("SELECT exercise_id FROM workout_exercise WHERE id=?", Long.class, retained);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM exercise WHERE id=?", catalogueId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(send("DELETE", first, null).statusCode()).isEqualTo(204);
        body(send("GET", first, null), 404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_exercise", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout_set", Integer.class)).isEqualTo(1);
        body(send("GET", "/api/sets/" + retainedSet, null), 200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM exercise WHERE id=?", Integer.class, catalogueId)).isEqualTo(1);
    }

    @Test
    void validatesDatesTimesMissingParentsAndPagination() throws Exception {
        body(send("POST", "/api/workouts", "{}"), 400);
        body(send("POST", "/api/workouts", "{\"date\":\"bad-date\"}"), 400);
        body(send("POST", "/api/workouts", "{\"date\":\"2026-10-22\",\"endedAt\":\"2026-10-22T10:00:00Z\"}"), 400);
        body(send("POST", "/api/workouts", """
                {"date":"2026-10-22","startedAt":"2026-10-22T12:00:00Z","endedAt":"2026-10-22T10:00:00Z"}
                """), 400);
        for (String path : new String[]{"/api/workouts/999999", "/api/workout-exercises/999999", "/api/sets/999999"}) {
            body(send("GET", path, null), 404);
            body(send("DELETE", path, null), 404);
        }
        String workout = createWorkout("2026-10-22");
        body(send("POST", workout + "/exercises", "{\"exerciseId\":999999}"), 404);
        body(send("POST", "/api/workouts/999999/exercises", "{\"exerciseId\":1}"), 404);
        body(send("POST", "/api/workout-exercises/999999/sets", "{}"), 404);
        body(send("GET", "/api/workouts/0", null), 400);
        body(send("GET", "/api/workouts?size=101", null), 400);
        body(send("GET", "/api/workouts?page=2147483647", null), 400);
    }

    @Test
    void databaseEnforcesPerformanceAndRelationshipConstraints() throws Exception {
        String workout = createWorkout("2026-10-22");
        long exercise = addExercise(workout);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO workout_set (workout_exercise_id, set_order, set_type, completed)
                VALUES (?, 0, 'WORKING', true)
                """, exercise)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO workout_set (workout_exercise_id, set_order, set_type, rpe)
                VALUES (?, 0, 'WORKING', 8.2)
                """, exercise)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO workout_exercise (workout_id, exercise_id, display_order)
                VALUES (999999, 1, 0)
                """)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void openApiDocumentsWorkoutAndEntryOperations() throws Exception {
        var paths = body(send("GET", "/v3/api-docs", null), 200).path("paths");
        assertThat(paths.path("/api/workouts").has("post")).isTrue();
        assertThat(paths.path("/api/workouts/{id}").has("delete")).isTrue();
        assertThat(paths.path("/api/workouts").path("post").path("responses").has("201")).isTrue();
        assertThat(paths.path("/api/workouts/{id}").path("delete").path("responses").has("204")).isTrue();
        assertThat(paths.path("/api/workout-exercises/{id}/sets").has("post")).isTrue();
        assertThat(paths.path("/api/sets/{id}").has("put")).isTrue();
    }

    private String createWorkout(String date) throws Exception {
        var response = send("POST", "/api/workouts", "{\"date\":\"" + date + "\"}");
        body(response, 201);
        return response.headers().firstValue("Location").orElseThrow();
    }

    private long addExercise(String workoutPath) throws Exception {
        long exerciseId = jdbc.queryForObject("SELECT id FROM exercise WHERE name='Bench Press'", Long.class);
        return body(send("POST", workoutPath + "/exercises", "{\"exerciseId\":" + exerciseId + "}"), 201).path("id").asLong();
    }

    private HttpResponse<String> send(String method, String path, String payload) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json").timeout(Duration.ofSeconds(20))
                .method(method, payload == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode body(HttpResponse<String> response, int expectedStatus) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(expectedStatus);
        if (expectedStatus >= 400) {
            assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
            assertThat(response.body()).doesNotContain("org.hibernate", "SQLException", "stackTrace");
        }
        return json.readTree(response.body());
    }
}
