# Java for Roope

This tutorial grows with Puntti-Urpo. You already know PHP, JavaScript, SQL and web development; the aim is to connect that experience to Java without hiding how the application works. Exercises are optional experiments: restore changes after trying them. Understanding has not yet been assessed. Milestones 1–5 are implemented; study them at your own pace. The sections follow the order concepts were introduced. Sections 1–6 introduce the original startup foundation; sections 7 onward explain the database, health checks and tests added afterward. Follow the README database/environment setup before running the application examples below.

## 1. Java, the JVM and the JDK

Java is a statically typed language. Like TypeScript, it checks types before execution. Unlike the usual TypeScript-to-JavaScript workflow, our Java compiler produces **bytecode**, not another source language. Bytecode is stored in `.class` files and executed by the **Java Virtual Machine (JVM)**. The JVM may compile frequently executed bytecode to native machine code while running.

The **JDK** (Java Development Kit) provides the compiler `javac`, runtime command `java`, and inspection tools such as `javap`. A **JRE** is the runtime environment needed to execute Java code; a full JDK includes runtime capabilities. We install a JDK because we develop and build the application. A separate JRE installation is unnecessary.

Actual example: Maven compiles `backend/src/main/java/com/punttiurpo/PunttiUrpoApplication.java` into `backend/target/classes/com/punttiurpo/PunttiUrpoApplication.class`. The generated `target/` directory is ignored by Git.

PHP also compiles to opcodes internally; the distinction here is the explicit build artifact and JVM runtime. Java is not JavaScript despite its name. A Spring Boot server typically remains alive across many requests, unlike the usual PHP request lifecycle.

**Why it matters:** the JDK version controls compilation and runtime compatibility. A Node installation cannot compile this application.

**Exercise:** from `backend/`, run:

```sh
java -version
javac -version
./mvnw compile
javap -classpath target/classes com.punttiurpo.PunttiUrpoApplication
```

**Verify:** the version commands report 25, the `.class` file exists, and `javap` lists the constructor and `main` method. This checks compiled bytecode rather than merely reading source.

## 2. Packages, imports and classes

Source: `backend/src/main/java/com/punttiurpo/PunttiUrpoApplication.java`.

```java
package com.punttiurpo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PunttiUrpoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PunttiUrpoApplication.class, args);
    }
}
```

`package com.punttiurpo;` gives the class a namespace, comparable to a PHP namespace. Under Maven's source root, the directory follows the package: `com/punttiurpo`. Imports let us use short type names; unlike a JavaScript import, they do not execute a module. Maven must separately make the dependency available.

`public class PunttiUrpoApplication` declares a named type. A public top-level class uses a matching filename. Braces enclose its members and statements end with semicolons. Java has objects and constructors like PHP and TypeScript, but we do not manually create an application object here. The compiler supplies a default no-argument constructor because none is declared.

**Why it matters:** package naming prevents collisions and defines the starting point for Spring's component discovery. Future application components will live under `com.punttiurpo`.

**Exercise:** run `javap` as above and identify the constructor even though no constructor appears in the source.

**Verify:** its declaration is `public com.punttiurpo.PunttiUrpoApplication();`. We will write explicit constructors when dependency injection appears in actual application code.

## 3. Reading the main method

Source: the same application class.

- `public` means the method is accessible from outside the class.
- `static` means it belongs to the class and can be called without constructing an instance. PHP's static methods are a useful comparison.
- `void` means it returns no value.
- `main` is the conventional JVM entry point used here.
- `String[] args` is an array of command-line strings. Java spells array types differently from TypeScript's `string[]` and uses capitalized `String` for its string class.
- `PunttiUrpoApplication.class` is a class object describing the type; it is not a new application instance.
- `SpringApplication.run(...)` hands startup to Spring Boot.

Spring Boot builds an application context, configures components and starts an embedded Tomcat server. The JVM remains alive because server threads continue running after `main` returns. You do not install a separate Tomcat or Apache server for this local application.

**Why it matters:** this is where application startup begins; it helps distinguish startup errors from HTTP request errors.

**Exercise:** temporarily add `System.out.println("Puntti-Urpo is starting");` immediately before `SpringApplication.run(...)`. Run `./mvnw spring-boot:run`.

**Verify:** the message appears once before the startup-complete log. Remove the statement afterward; ordinary application logging will use a logger when we introduce it.

## 4. Annotations and Spring Boot startup

Source: `@SpringBootApplication` on the application class.

An annotation is metadata attached to a declaration. It resembles a PHP attribute; it is not itself an ordinary method call. Framework code interprets it. `@SpringBootApplication` combines configuration, auto-configuration and component scanning. Auto-configuration selects defaults based on dependencies and configuration. Component scanning discovers application components in this package and its children.

Our Web MVC starter supplies the web infrastructure. There are no controllers yet, so an incoming request reaches the web framework but finds no matching application route and receives HTTP 404.

**Why it matters:** annotations have concrete framework behavior. We will explain each new annotation when it enters the code rather than treating it as magic.

**Exercise:** start the app and request `/` with `curl -i http://127.0.0.1:8080/`.

**Verify:** HTTP 404 means the server answered. A connection-refused error means no server accepted the connection at that address. Neither result establishes database health; the completed milestone adds `/actuator/health` for that purpose.

## 5. Maven, dependencies and the wrapper

Sources: `backend/pom.xml`, `backend/mvnw`, and `backend/.mvn/wrapper/maven-wrapper.properties`.

The POM (Project Object Model) describes the project, dependency coordinates and build plugins. `groupId`, `artifactId` and `version` identify an artifact, somewhat like a Composer package or npm package name plus version. The Spring Boot parent supplies compatible dependency and plugin defaults. The Web MVC starter groups dependencies needed for a web application. A starter is dependency configuration, not generated business code.

`java.version` selects Java 25 for the build. The Spring Boot Maven plugin creates an executable JAR containing our compiled code and runtime dependencies. A normal library JAR does not necessarily contain everything needed to run a server.

The wrapper script downloads and runs Maven 3.10.0. It does **not** install Java. Its properties pin Maven and its distribution checksum. Dependencies are cached under `~/.m2/`; wrapper distributions normally live under `~/.m2/wrapper/dists/`. The POM is not identical to `package-lock.json`: it declares and manages versions but is not a complete transitive lockfile.

Maven lifecycle phases include `compile`, `test`, `package` and `verify`. Running `verify` runs preceding phases too. The initial startup checkpoint had no tests. Completed Milestone 1 now includes three PostgreSQL integration tests, so `verify` requires Docker and runs those tests before packaging. Tests supply their own database configuration; application startup still needs the Compose configuration from the README.

**Why it matters:** colleagues can run the same Maven version without installing it globally, and the build is explicit and inspectable.

**Exercise:** run `./mvnw --version`, then `./mvnw verify` and `java -jar target/puntti-urpo-0.0.1-SNAPSHOT.jar`.

**Verify:** Maven reports 3.10.0 and Java 25, the build succeeds, and the packaged server responds with 404 at `/`. Stop it with Ctrl+C.

## 6. Configuration and a startup exercise

Source: `backend/src/main/resources/application.properties`.

`server.address=127.0.0.1` binds the server to the local machine. `server.port=${SERVER_PORT:8080}` reads an environment variable, falling back to 8080. This placeholder is interpreted by Spring, not by Java source syntax. Error settings prevent messages, binding details and stack traces from being included in default HTTP error responses.

**Why it matters:** environment configuration changes runtime behavior without editing or recompiling source. Binding locally keeps this unfinished server off other machines' network interfaces.

**Exercise:** from `backend/`, run `SERVER_PORT=18080 ./mvnw spring-boot:run` and request `http://127.0.0.1:18080/` from another terminal.

**Verify:** the startup log shows port 18080 and curl receives HTTP 404 from that port. Stop the process. The properties file should remain unchanged.

## Checkpoint 1 comprehension questions

1. What does Maven compile a `.java` file into, and what executes that result?
2. Why can the JVM call our `main` method without first constructing `PunttiUrpoApplication`?
3. Why is HTTP 404 an expected result now, and what would a connection-refused error tell you instead?

## Later lessons

The next sections introduce integration testing, dependency injection, generics, lambdas and exception handling where they occur in this milestone. Business services, controllers, repositories, JPA, transaction boundaries, records, enums, streams and validation remain future lessons; no source examples are invented ahead of implementation.

## 7. Environment configuration: two processes, one agreement

Sources: `.env.example`, `compose.yaml`, and `backend/src/main/resources/application.properties`.

The Java application and PostgreSQL are separate processes. Java does not become a database because a JDBC dependency is installed. It needs a network address, database name, username and password to connect to PostgreSQL. Our local Java process reaches the container through the loopback port published by Docker.

```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:127.0.0.1}:${DB_PORT:54328}/${POSTGRES_DB:punttiurpo}?connectTimeout=3&socketTimeout=3
spring.datasource.username=${POSTGRES_USER:punttiurpo}
spring.datasource.password=${POSTGRES_PASSWORD}
```

The `${NAME:default}` syntax belongs to Spring property resolution. With no colon/default, the password is required. The JDBC URL identifies the protocol/driver, server, port and database. Its final query parameters bound connection and socket waits in seconds. Hikari's separately configured connection/validation timeouts use milliseconds.

Compose reads `.env` to configure the database. Java does not automatically read that file: `set -a; source .env; set +a` in the README exports the same settings for the child Java process. This is comparable to PHP's environment configuration or Node's `process.env`, without a custom dotenv dependency. Only source files you trust, because `source` executes shell syntax. The example password is intentionally empty; the implementation machine has an ignored generated value.

**Why it matters:** exporting a setting and editing a source file are different actions. Incorrect ports or stale credentials cause connection failures even if both processes are alive.

**Exercise:** after the README setup, identify which connection values have defaults and which do not. Run `docker compose ps` from the repository root and check the published port without printing the password.

**Verify:** only this project's database appears in the Compose output, bound to `127.0.0.1` on `DB_PORT`. `/actuator/health` returns 200 after Java starts with matching settings.

## 8. JDBC, connection pools and typed SQL results

Sources: `backend/pom.xml` and `backend/src/test/java/com/punttiurpo/FoundationIntegrationTest.java`.

JDBC is Java's standard database connectivity API. The PostgreSQL driver implements database-specific communication, roughly the role of a PDO driver in PHP. HikariCP maintains a pool of reusable connections rather than establishing a new TCP/database connection for every operation. Spring's `JdbcTemplate` removes repetitive JDBC resource handling; it does not turn SQL into JPA entities.

A real test executes:

```java
assertThat(jdbc.queryForObject("SELECT version()", String.class))
        .startsWith("PostgreSQL 18.6");
```

`jdbc` is a `JdbcTemplate` object supplied by Spring. `queryForObject` is an instance method; unlike `SpringApplication.run`, we call it on an object rather than a class. `String.class` tells the template the requested result type. The result is asserted against the database version; this proves an actual SQL round trip, not just the existence of configuration.

**Why it matters:** Java's typed result handling makes expected data explicit. Later, we will compare JDBC with JPA using real application entities rather than adding entities now.

**Exercise:** run `./mvnw -Dtest=FoundationIntegrationTest#connectsToRealPostgresql test` from `backend/` with Docker running.

**Verify:** one test passes against a disposable PostgreSQL container. It does not need the Compose database or your local database password.

## 9. Flyway: executable schema history

Sources: `backend/src/main/resources/application.properties`, `backend/src/main/resources/db/migration/README.md` and the `appliesCatalogueMigrationsAndValidatesSchema` test in `FoundationIntegrationTest.java`.

Flyway runs versioned SQL files in order and records applied migrations with checksums in `flyway_schema_history`. Think of a deliberately recorded sequence of schema changes, similar to migration tooling in PHP frameworks. This is separate from inserting workout records.

Milestone 1 initially had no domain migrations and only the history table. Milestone 2 now adds V1 (exercise schema) and V2 (Finnish seed data). The migration folder's README is documentation, not an SQL migration. The current foundation test checks four applied migrations, none pending and the catalogue/workout/history tables.

`spring.flyway.clean-disabled=true` disables Flyway's destructive clean operation. Validation checks consistency between available and applied migrations. The catalogue migrations now provide the first real schema and seed changes. Add a new migration for later changes; do not modify applied shared migration files.

**Why it matters:** migrations let another developer or deployment reproduce the intended schema. Allowing an ORM to change shared schemas silently would hide an important learning and operational boundary.

**Exercise:** start the local database/application using the README, then open psql using the command in `docs/DATABASE.md`. Run `SELECT count(*) FROM flyway_schema_history;` and `\dt`.

**Verify (current Milestone 3):** the history row count is four, and the public tables are `flyway_schema_history`, `exercise`, `workout`, `workout_exercise` and `workout_set`. Both V1 and V2 are real migrations; no placeholder migration is needed.

## 10. An actual Spring integration test

Source: `backend/src/test/java/com/punttiurpo/FoundationIntegrationTest.java`.

A unit test checks a small piece of behavior in isolation. An integration test checks components working together. These tests start the real Spring application, web server and PostgreSQL; no mocked database stands in for PostgreSQL.

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FoundationIntegrationTest {
```

`@SpringBootTest` loads the application context. `RANDOM_PORT` starts a real HTTP server on a dynamically selected port, avoiding conflicts with your usual development server. `@Testcontainers` manages annotated container fields. The `static final` PostgreSQL container is shared by the class's tests. `final` prevents assigning a different object to that field; it does not make the container immutable or prevent pausing it.

`@Test` marks a method for JUnit execution. Test methods can omit `public`; package-private visibility is sufficient here. Java's `void` means these methods return no value; failed assertions signal failure by throwing an assertion error.

```java
@DynamicPropertySource
static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

This registers test-specific connection settings before Spring configures its database components. The method reference `postgres::getJdbcUrl` supplies a function to be called for the value, comparable to `() => postgres.getJdbcUrl()` in TypeScript. It is not the URL string itself. No local `.env` values need to leak into this test database.

The PostgreSQL container uses `withUrlParam` for JDBC timeout parameters. That API builds the URL correctly, including any existing query parameters, instead of manually concatenating a second `?`.

**Why it matters:** a meaningful green test proves the application can work with the intended database engine, while random ports and dedicated containers keep tests independent of local project data. Tests deliberately fail if Docker is unavailable.

**Exercise:** stop this project's Compose database, leave Docker Desktop running, and run `./mvnw verify` from `backend/`.

**Verify (current Milestone 4):** all 67 tests pass. This demonstrates test isolation from the Compose database. A successful test build creates the executable JAR too.

## 11. Dependency injection in the test context

Source: the `JdbcTemplate jdbc`, `Flyway flyway` and `int port` fields in `FoundationIntegrationTest.java`.

```java
@Autowired
JdbcTemplate jdbc;
```

Spring creates/configures a `JdbcTemplate` and assigns it to this test field. This is dependency injection: the object receives a dependency rather than creating one itself. In familiar terms, a PHP container might resolve a configured service for a test, or a TypeScript test harness might provide a dependency.

The test uses field injection for brevity within the Spring-managed test lifecycle. This is not the pattern we will use for application services: constructor injection will make their required collaborators explicit and easy to instantiate in isolated tests. There are no application services or custom constructors to demonstrate yet.

`@LocalServerPort` supplies the actual dynamically assigned HTTP port. Your test must use that port rather than assuming 8080.

**Why it matters:** the injected objects are real configured components. Inspecting this test teaches what Spring supplies without inventing an extra application layer merely to demonstrate injection.

**Exercise:** locate the three injected test fields and explain where each value/object comes from.

**Verify:** `JdbcTemplate` and `Flyway` are Spring beans backed by the test datasource; the integer comes from the server started on a random port. These values are not created by the test field declarations alone.

## 12. Generics and fluent assertions

Source: `FoundationIntegrationTest.java`, especially `HttpResponse<String>` and `jdbc.queryForList(..., String.class)`.

```java
HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
assertThat(response.statusCode()).isEqualTo(statusCode);
assertThat(response.body()).isEqualTo(body);
```

`HttpResponse<String>` is a generic type: the response body is known to be a string. This is similar to TypeScript's `Promise<T>` or a typed API result, although Java's type system and runtime differ. Our `BodyHandlers.ofString()` choice agrees with that type. `response.body()` is therefore available as a `String`, not an untyped value you must guess about.

The schema test uses `queryForList` to obtain a list of strings. AssertJ's `containsExactly` checks that the returned table names are exactly the expected content. The chained assertions read as expectations, not conditional branches that silently ignore a mismatch.

**Why it matters:** generics communicate what a container or wrapper holds and let the compiler catch incompatible use. Assertions turn expectations into executable failures.

**Exercise:** temporarily change the expected PostgreSQL version assertion to a deliberately wrong major version and run only `connectsToRealPostgresql`.

**Verify:** the test fails with expected versus actual text. Restore the assertion to PostgreSQL 18.6 and confirm it passes. A test is useful because it can detect failure, not merely because it executes code.

## 13. Exceptions, finally and database failure

Source: `reportsHealthDuringDatabaseOutageAndRecoveryWithoutExposingDetails` and `assertHealth` in `FoundationIntegrationTest.java`.

The JDK HTTP client can throw checked exceptions for I/O or interruption. Java requires such exceptions to be caught or declared; our test helper declares `throws Exception`, allowing JUnit to report unexpected failures. Production code will need more precise error handling when application use cases appear. PHP/JavaScript do not have the same checked-exception declaration requirement.

```java
postgres.getDockerClient().pauseContainerCmd(postgres.getContainerId()).exec();
try {
    assertHealth(503, "{\"status\":\"DOWN\"}");
} finally {
    postgres.getDockerClient().unpauseContainerCmd(postgres.getContainerId()).exec();
}
```

Pausing freezes the test database process while preserving the container and port. JDBC times out when it cannot receive a response. Actuator reports DOWN, and the HTTP layer returns 503. `finally` attempts to unpause even if the assertion fails, preventing the test from leaving its database frozen during cleanup. Testcontainers removes its own resources when the tests finish.

The complete test checks healthy → unavailable → healthy. Exact body assertions also prove no component details or exception trace escaped into the response. Server-side diagnostic warnings during this deliberate outage are expected and are different from information exposed to clients.

**Why it matters:** real dependency failures can leave Java running while the application cannot perform useful work. A process-exists check alone would miss that problem. Initial database failure is different: Flyway prevents successful startup, so no running health endpoint exists yet.

**Exercise:** with the local application running, stop only this project's Compose database, call `/actuator/health`, then restore it with `docker compose up -d --wait db` and call health again.

**Verify:** responses change from 200 UP to 503 DOWN and back to 200 UP without restarting Java. Keep the database available afterward if you want to continue using the app.

## Milestone 1 review

Read `PunttiUrpoApplication.java`, `application.properties`, `pom.xml`, `compose.yaml` and `FoundationIntegrationTest.java` in that order. The documentation explains implemented behavior; future domain features remain explicitly planned.

Three questions for later discussion:

1. What do the JDK, Maven Wrapper and Spring Boot each contribute to starting this application?
2. Why can the JVM remain running while `/actuator/health` returns 503, but fail to start if PostgreSQL is unavailable initially?
3. How do our tests connect to their own PostgreSQL database without using your local `.env` or Compose volume?

Suggested exercise: perform the health outage/recovery sequence above and explain each response using the source and configuration. Completing it is for your learning, not a prerequisite imposed before finishing this milestone. Milestone 2 was subsequently approved and is covered next; approval is still required before each later milestone.

# Milestone 2: a working exercise catalogue

Milestone 1 chapters above explain the foundation as it was introduced. Milestone 2 introduced the exercise table and two migrations; the current schema now has four migrations and workout tables as well; the earlier database exercises have been updated to match that schema. Use the following chapters for the current implementation. You can study the completed milestone before doing any exercises.

## 14. A real class, constructors, objects and access modifiers

Source: `backend/src/main/java/com/punttiurpo/entity/Exercise.java`.

`Exercise` is a mutable Java class describing persisted exercise state. `private` fields cannot be assigned by ordinary code in another class. Public getter methods expose values, while `update(...)` explicitly changes editable fields. This is familiar from a PHP object with private properties; unlike a plain JavaScript object, callers cannot simply assign any arbitrary property.

```java
public Exercise(String name, String category, String primaryMuscleGroup,
                String equipment, String notes) {
    update(name, category, primaryMuscleGroup, equipment, notes);
}
```

The public constructor describes how application code creates a custom exercise. `new Exercise(...)` allocates and initializes an object. `this.name = name` inside `update` assigns the parameter to the current object's field, like PHP's `$this->name = $name` or TypeScript's `this.name = name`.

The separate `protected Exercise()` no-argument constructor exists for JPA, which reconstructs entities from database rows. It is not the application creation API. `protected` permits access within the package and through subclass rules. The `custom` field defaults to true for new application-created objects; JPA reads false from built-in rows. There is no API setter for changing origin.

`Long id` is an object wrapper, so it can be null before insertion. Primitive `long` cannot hold null. After PostgreSQL assigns an identity, Hibernate fills the ID. The primitive `boolean custom` always has a true/false value.

**Why it matters:** constructors and access modifiers make intended operations visible instead of allowing callers to mutate every property indiscriminately. The entity is not a request DTO.

**Exercise:** locate both constructors and explain which caller uses each. Create a temporary custom instance in a scratch test and inspect `getId()` and `isCustom()` before persistence.

**Verify:** its ID is null and custom is true. The HTTP creation test shows that persistence supplies an ID. Remove any scratch experiment afterward.

## 15. JPA annotations and Hibernate's responsibility

Sources: `Exercise.java`, `backend/src/main/resources/db/migration/V1__create_exercise.sql` and `backend/src/main/resources/application.properties`.

JPA (Jakarta Persistence) defines persistence interfaces/annotations; Hibernate is the implementation used here. `@Entity` makes the class persistent, `@Table` names its table, `@Id` identifies its primary key, and `@Column` describes column mapping. `@GeneratedValue(strategy = GenerationType.IDENTITY)` matches PostgreSQL's identity-generated primary key.

`GenerationType.IDENTITY` is an enum constant supplied by Jakarta Persistence: an enum defines a fixed set of named choices. We have not invented application metadata enums yet because category, muscle group and equipment remain free text.

Hibernate translates entity operations into SQL and reconstructs rows as Java objects. It does not replace schema design. Flyway creates the actual constraints and unique expression index. `spring.jpa.hibernate.ddl-auto=validate` checks mappings against the migrated schema instead of altering tables. `spring.jpa.open-in-view=false` prevents leaving a persistence session open merely to support serialization after the service returns.

**Why it matters:** JPA mapping and SQL constraints solve related but distinct problems. An annotation is not proof that a database constraint exists; read the migration too.

**Exercise:** compare the `primaryMuscleGroup` field's annotation with the SQL `primary_muscle_group` column. Inspect the unique name index through psql's `\d exercise`.

**Verify:** the column names and lengths agree, and the unique index uses `lower(name)`. Case-insensitive uniqueness is enforced by PostgreSQL, even for writes outside the REST API.

## 16. Interfaces, generics, inheritance and Optional

Source: `backend/src/main/java/com/punttiurpo/repository/ExerciseRepository.java` and `service/ExerciseService.java` under the same Java source root.

```java
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
```

An interface declares a contract. As with PHP interfaces or TypeScript interfaces, callers rely on available operations, although Java interfaces also have features such as default methods. `JpaRepository<Exercise, Long>` uses generic parameters to say this repository stores Exercise entities identified by Long IDs. `extends` here inherits repository operations from another interface; we are not writing a hierarchy of business entity subclasses.

Spring Data creates a runtime proxy implementing this interface. Methods such as `findById`, `saveAndFlush` and `flush` come from the repository contract. The explicit `@Query` supplies JPQL for our search. JPQL refers to Java entity fields (`e.name`), not directly to a table name. Values are bound as parameters; input is not concatenated into SQL. `locate` checks a literal substring, so `%` does not silently become a wildcard.

```java
return repository.findById(id).orElseThrow(ExerciseNotFoundException::new);
```

`findById` returns `Optional<Exercise>`: either an exercise or no value. This makes absence explicit rather than returning a row object sometimes and an unexplained null other times. `orElseThrow` extracts the object or throws our domain exception. `ExerciseNotFoundException::new` is a constructor reference supplying the exception when needed.

**Why it matters:** you should know where inherited repository methods come from and which part Spring implements. Optional does not mean every field should be wrapped in Optional; nullable optional text remains ordinary nullable fields here.

**Exercise:** trace `GET /api/exercises/999999` from controller to `findById` to `orElseThrow` and the exception advice.

**Verify:** it produces a 404 ProblemDetail response, while an invalid ID such as `0` is rejected with 400 before the service lookup. The API integration tests check both cases.

## 17. Composition and constructor dependency injection

Sources: `service/ExerciseService.java` and `controller/ExerciseController.java`.

```java
private final ExerciseRepository repository;

public ExerciseService(ExerciseRepository repository) {
    this.repository = repository;
}
```

The service composes a repository dependency: it *has a* repository rather than *is a* repository. Its single public constructor tells Spring exactly what it needs. Spring injects a configured implementation; no `@Autowired` is necessary on a sole constructor. `final` prevents reassigning the dependency field after construction, but does not make the repository or database immutable.

The controller similarly receives an `ExerciseService`. These classes do not construct a new connection pool or repository for each request. Compared to service-locator code in PHP, dependencies are visible in the constructor rather than retrieved invisibly from a global container.

**Why it matters:** the constructor makes requirements clear, and a unit test can instantiate the service with a substitute repository. This is the promised application counterpart to the test-field injection shown in Milestone 1.

**Exercise:** read the constructor setup in `src/test/java/com/punttiurpo/service/ExerciseServiceTest.java` and locate the manually created service.

**Verify:** the unit test needs neither Spring startup nor Docker. The full integration tests still use real Spring-managed objects and PostgreSQL, so mocks do not stand in for persistence behavior.

## 18. Records, normalization and Bean Validation

Sources: `dto/ExerciseRequest.java`, `dto/ExerciseResponse.java`, `dto/ExercisePage.java` and `mapper/ExerciseMapper.java` under `backend/src/main/java/com/punttiurpo/`.

A Java record is a concise data carrier. The compiler provides a constructor, accessors such as `name()`, value-based equality, hashCode and toString. It resembles a TypeScript data shape but is an actual runtime Java type; its components cannot be reassigned after construction. Record immutability is shallow: a component holding a mutable collection is not automatically a deeply immutable collection.

`ExerciseRequest` has a compact constructor that normalizes component values before the record is initialized. `normalize` strips surrounding whitespace and converts blank optional values to null. Then `@Valid` on the controller's request parameter triggers Bean Validation. `@NotBlank` and `@Size` express field constraints. Validation is not implicit whenever any Java code constructs a record; the controller boundary (or an explicit Validator call in a test) triggers it.

The input record has no ID or custom-origin field. The response record deliberately includes those output-only values. The explicit mapper creates the response from an entity; clients receive no Hibernate internals or accidental entity properties.

`ExercisePage` uses `List<ExerciseResponse>` and count/page metadata. The repository's Spring Page is not exposed directly, keeping the JSON contract under our control.

**Why it matters:** persistence objects and HTTP data evolve for different reasons. A separate DTO prevents unintentional changes to API contracts when the entity changes.

**Exercise:** run `./mvnw -Dtest=ExerciseRequestTest test`. Inspect the valid-at-limit and invalid-over-limit cases for all five text fields.

**Verify:** whitespace normalization, blank-name rejection and length boundaries pass without Spring or Docker. Next compare a POST body's editable fields with the output response; custom origin is assigned by the server.

## 19. Transactions, dirty checking and rollback

Source: `service/ExerciseService.java`, especially the create/update methods.

`@Transactional(readOnly = true)` on the service supplies the default for public read operations. Write methods have their own `@Transactional`, overriding that default. Spring normally applies this through a proxy when another bean calls the service. A private helper or a method calling another method on the same object does not create a separate proxy interception.

A write transaction groups database changes so they either commit together or roll back. In `update`, `find` loads a managed entity, we reject built-in origin, and `exercise.update(...)` changes fields. Hibernate dirty checking detects those changes. Another explicit `save` is unnecessary for the managed entity.

`repository.flush()` synchronizes pending changes with SQL, making constraint failures visible at this point; **flush is not commit**. If an unchecked database exception escapes, Spring rolls back the transaction. PostgreSQL's unique index is therefore the final defense against duplicate names, including concurrent requests. There is no optimistic version column yet; simultaneous edits can overwrite earlier values.

**Why it matters:** a failed rename must not leave a partially edited exercise. Java object changes alone are not proof of a committed database write.

**Exercise:** create a custom exercise, then PUT its name to an existing built-in name with different capitalization. GET it again.

**Verify:** the PUT returns 409 and the original name remains. The integration test `rejectsDuplicateNamesAndRollsBackConflictingUpdate` proves this using PostgreSQL, not a mocked repository.

## 20. Streams, method references and local type inference

Sources: `service/ExerciseService.java` and `exception/ApiExceptionHandler.java`.

`var result = repository.search(...)` uses local type inference: the compiler determines the concrete type from the expression. It is not JavaScript's mutable/dynamically typed `var`; Java still checks all operations and incompatible assignments at compile time.

`result.map(ExerciseMapper::toResponse)` converts a page of entities into a page of DTOs. Method references pass behavior without invoking it immediately. In the exception advice, `getFieldErrors().stream().map(...).toList()` maps validation failures into safe field/message records. This resembles JavaScript's array `.map`, while a Java Stream is a processing pipeline rather than a stored collection itself.

**Why it matters:** small mapping pipelines keep code readable when the transformation is simple. They should not hide transaction boundaries or business decisions inside clever chains.

**Exercise:** explain the input and output types for the mapper reference and for the field-error stream.

**Verify:** the first maps `Exercise` to `ExerciseResponse`; the second maps Spring field errors to our safe `FieldError` record, omitting rejected values. A blank-name POST produces the documented `errors` array.

## 21. HTTP controllers, exceptions and structured logs

Sources: `controller/ExerciseController.java` and `exception/ApiExceptionHandler.java`.

`@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping` and `@PutMapping` bind methods to HTTP routes. `@RequestParam` binds search/page inputs, `@PathVariable` binds an ID, and `@RequestBody` deserializes JSON. Method constraints validate IDs and page limits through Spring MVC. POST returns a `ResponseEntity` to control the 201 status and Location header. PUT is full replacement of editable fields, so omitted optional fields become null.

`ExerciseNotFoundException` and `BuiltInExerciseException` extend `RuntimeException`, representing distinct application failures. `@RestControllerAdvice` centralizes their translation to 404/403 ProblemDetail bodies. `ResponseEntityExceptionHandler` supplies conventional handling for framework request errors; we override validation, parsing and type mismatch messages. Database integrity conflicts become 409. An unexpected error gets a generic 500 body while a private server log retains diagnostic details.

Console logs now use Spring Boot's ECS JSON format, configured in `application.properties`. Structured logs have named fields (level, logger, message and service context) instead of requiring text-position parsing. They are operational evidence, not safe public portfolio content; review before sharing.

**Why it matters:** throwing a domain exception and deciding an HTTP response are separate responsibilities. SQL exceptions and internal stack traces are not useful or safe client contracts.

**Exercise:** POST a blank name, GET an unknown ID, and attempt to edit a built-in exercise. Compare status, Content-Type and response structure.

**Verify:** 400, 404 and 403 respectively, with `application/problem+json`. The validation case identifies the field without echoing rejected input; none expose a stack trace.

## 22. Unit tests, integration tests and scope boundaries

Sources: `src/test/java/com/punttiurpo/service/ExerciseServiceTest.java`, `dto/ExerciseRequestTest.java` and `ExerciseApiIntegrationTest.java` under `backend/`.

Mockito creates repository doubles for service-policy tests. We explicitly enable its subclass mock maker in `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`, avoiding runtime Java-agent attachment. These tests do not need final-class mocking. A mock verifies decisions such as refusing a built-in edit; it does not verify real SQL, transactions or constraints.

The HTTP integration suite starts real PostgreSQL and checks creation/read/replace, normalized fields, pagination/search, validation, seed protection, uniqueness, rollback, SQL constraints and Swagger compatibility. Each suite owns its disposable database. Cleanup SQL removes only custom rows in that test database; no delete endpoint or development-volume cleanup is implied.

**Why it matters:** different tests prove different claims. A repository mock cannot establish that PostgreSQL enforces `lower(name)` uniqueness. A passing HTTP test also does not imply a production security or accessibility review has happened.

**Exercise:** run `./mvnw -Dtest=ExerciseServiceTest,ExerciseRequestTest test` without needing Docker, then run `./mvnw verify` with Docker running.

**Verify:** five focused unit tests pass; current full verification runs 67 tests across eight classes and packages the application. Current features, including the later workout domain, remain backend-only; there is no gym UI yet.

## Milestone 2 review questions and exercise

1. Why is `Exercise` a mutable JPA entity while HTTP requests/responses are records, and how does the mapper connect them?
2. How does constructor injection let a service unit test run without Spring, while the integration suite still verifies real PostgreSQL behavior?
3. Why does a conflicting rename roll back even though the managed Java object's fields were changed before `flush()`?

Suggested exercise: use Swagger UI to create a custom exercise, search for it, replace it with PUT, and provoke a duplicate-name 409. Trace one successful request and one failed request through the controller, service, repository, migration constraint and exception advice. Do not add a deletion endpoint; workout reference/deletion rules belong to Milestone 3.

# Milestone 3: logging real workout structure

## 23. Foreign keys and actual JPA relationships

Sources: `backend/src/main/java/com/punttiurpo/entity/Workout.java`, `WorkoutExercise.java`, `WorkoutSet.java`, and `backend/src/main/resources/db/migration/V4__create_workout_domain.sql`.

Workout represents a session. WorkoutExercise represents one occurrence of a catalogue exercise inside that session. WorkoutSet belongs to that occurrence, not directly to the reusable catalogue exercise. This means two workouts using Bench Press do not share their sets. It also allows the same exercise twice in a workout, for example at separate points in the session.

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "workout_id", nullable = false)
private Workout workout;
```

Many logged entries may reference one workout, so the Java association is ManyToOne. `JoinColumn` names the foreign-key column. The SQL `REFERENCES workout(id)` enforces that the parent exists. A Java field declaration alone does not establish database integrity; read both sides of the mapping.

LAZY asks Hibernate to load the referenced object when needed rather than eagerly fetching every related object. A proxy can represent the relationship initially. DTO mapping stays inside the service transaction because OSIV remains disabled; it does not rely on JSON serialization to trigger database work afterward.

**Why it matters:** references determine which data belongs together and which operations can be performed safely. A catalogue ID, workout ID and logged-exercise ID are different identities even if their numeric values sometimes coincide.

**Exercise:** in Swagger, create a workout, add Bench Press twice, and inspect its detail response.

**Verify:** the two entries have different logged-exercise IDs and display orders, while their nested catalogue exercise IDs match. Add a set to one entry; it should not appear under the other.

## 24. Database cascades are not JPA cascades

Sources: V4 SQL, `service/WorkoutService.java`, and `src/test/java/com/punttiurpo/WorkoutApiIntegrationTest.java` under `backend/`.

SQL `ON DELETE CASCADE` lets PostgreSQL remove dependent rows when the referenced parent is deleted. Our workout-to-entry and entry-to-set foreign keys use it. Catalogue references instead use `ON DELETE RESTRICT`: an exercise cannot be removed from the database while logged entries still reference it.

There are deliberately no bidirectional parent collections, `CascadeType.REMOVE` annotations or JPA orphan-removal settings. The service deletes one managed parent and flushes; PostgreSQL handles descendants. This is comparable to using SQL foreign-key behavior through a PHP data layer rather than looping over every child in application code.

Deleting a logged exercise does not delete its catalogue record or sibling entries. Deleting a set affects only that set. Database constraints also enforce this outside the REST API. These endpoints are permanent deletion, not a trash/restore workflow.

**Why it matters:** a cascade on the wrong relationship could erase shared catalogue data or another workout. The direction of ownership is more important than the convenience of a one-line delete call.

**Exercise:** create a disposable workout with two logged entries and a set under each. Delete one logged entry, then inspect the workout. Finally delete only your disposable workout.

**Verify:** the first deletion removes only that entry and its sets; its sibling stays. The final deletion removes that workout's descendants but preserves catalogue exercises. The automated integration tests verify both isolation cases against PostgreSQL.

## 25. Exact decimals with BigDecimal

Sources: `dto/WorkoutSetRequest.java`, `entity/WorkoutSet.java`, the `validateSet` method in `service/WorkoutService.java`, and V4 SQL.

Java `double`, like JavaScript's Number, uses binary floating-point. It cannot represent every decimal fraction exactly. For kg and RPE we use BigDecimal, backed by PostgreSQL numeric columns. Construct decimal constants from strings, such as `new BigDecimal("0.5")`, instead of introducing a floating-point approximation first.

`numeric(6,2)` stores a weight with six total digits, two after the decimal point: maximum 9999.99. `numeric(3,1)` stores RPE. Bean Validation's `@Digits` rejects excessive input precision before persistence, rather than relying on PostgreSQL's normal numeric coercion/rounding. The RPE range and half-step checks exist in both application logic and SQL.

```java
request.rpe().remainder(new BigDecimal("0.5")).signum() != 0
```

This checks whether the entered RPE is an exact multiple of 0.5. It is validation, not a strength estimate. BigDecimal is immutable: operations return values rather than modifying the number in place. Its `equals` also considers scale (`8.0` and `8.00` differ by equals); for numerical equality, use `compareTo` or AssertJ's `isEqualByComparingTo` as the tests do.

**Why it matters:** precision choices are part of the data contract, not merely formatting. Derived calculations later must not silently convert these values through double.

**Exercise:** submit a completed set with weight 82.25, six repetitions and RPE 8.5. Then try RPE 8.2 and weight 82.251.

**Verify:** the first persists exactly and the latter two return 400. GET the accepted set and inspect the decimal values. Milestone 4 now exposes both derived fields; they are explained in chapters 31–35 below.

## 26. Date-only values, instants and time validation

Sources: `dto/WorkoutRequest.java`, `entity/Workout.java`, V4 SQL and `validateTimes` in WorkoutService.

LocalDate represents a calendar date without a time or timezone. It is suitable for the workout's chosen training day. Instant represents a point on the global timeline. JSON start/end values include an offset such as `Z`; PostgreSQL stores them as `timestamp with time zone`, preserving the instant rather than the original timezone label. A Finnish user might view a stored UTC instant in Helsinki time later, but the explicitly selected training date does not silently change.

Both timestamps are optional. If end exists, start must exist and end cannot be earlier. We check this cross-field rule in the service and repeat it as a SQL CHECK. Equal start and end are permitted. Future dates are not automatically rejected, allowing preparation of a session.

**Why it matters:** a server's local timezone should not decide which calendar day your workout belongs to. Java date/time types express different concepts; a single generic date string would obscure that distinction.

**Exercise:** POST a workout with a date and no times, then try a second request with an end but no start.

**Verify:** the first returns 201 and the second 400. Add an ordered pair of offset-bearing timestamps and verify a GET returns the corresponding instants. Do not depend on nanosecond precision: PostgreSQL timestamps normally preserve microseconds.

## 27. Enum values, nullable input and draft defaults

Sources: `entity/WorkoutSetType.java`, `dto/WorkoutSetRequest.java` and `entity/WorkoutSet.java`.

WorkoutSetType is our first application enum: a closed set of WARMUP and WORKING. `@Enumerated(EnumType.STRING)` stores the names in the database, rather than numeric positions that could change meaning when enum order changes. Unknown JSON enum values produce 400.

The request's completion flag uses Boolean, which may be null while decoding absent input. Its compact constructor defaults null to false, and defaults absent set type to WORKING. The entity and response use primitive boolean, so persisted completion is never null. Tests caught Jackson 3's strict handling of an omitted primitive record argument: making the *input* optional and normalizing it fixes the contract without globally relaxing JSON parsing.

A draft `{}` set has no weight, repetitions or RPE. Completing it requires an explicit nonnegative load and positive repetitions. Zero kg is valid; we do not infer or multiply dumbbell load per hand. PUT replaces the performance values, so omitted values clear and completion defaults false. Omitted order is the deliberate exception: it preserves a set's position during updates.

**Why it matters:** omitted, null, zero and false carry different meanings. Define defaults explicitly instead of hoping a parser's behavior matches your intent.

**Exercise:** create a draft set with `{}`, then PUT weight/repetitions and `"completed":true` to its returned ID.

**Verify:** the draft returns false/WORKING with null performance fields; the update returns true and the recorded values. Setting completed true without weight/repetitions returns 400.

## 28. Nested DTOs, maps and avoiding repeated database queries

Sources: the `response(Workout)` method in `service/WorkoutService.java`, `repository/WorkoutExerciseRepository.java`, `repository/WorkoutSetRepository.java` and `mapper/WorkoutMapper.java`.

A workout detail response needs a list of logged exercises, each with a list of sets. Calling the set repository separately for every entry would create one extra query per entry: a common N+1 problem. Instead, we fetch the workout's entries (including catalogue exercise data through `@EntityGraph`) and the workout's sets once.

The service groups the sets into `Map<Long, List<WorkoutSetResponse>>`, keyed by logged-exercise ID. `Collectors.groupingBy` forms the groups and `Collectors.mapping` maps entities to DTOs. `getOrDefault(id, List.of())` supplies an empty list for an entry without sets. This is similar to grouping an array into a JavaScript Map, but the Java compiler knows both the key and element types.

Lists preserve the repository's set-order sort within each group. The returned nested records contain DTOs, never parent/child entity references, so serialization cannot recurse through a bidirectional object graph. Summary pagination intentionally omits children altogether.

**Why it matters:** readable response assembly can also make query costs predictable. It is not necessary to expose entity collections or introduce a generic graph-loading abstraction.

**Exercise:** trace how two sets with the same setOrder but different logged-exercise IDs are assigned to their correct response groups.

**Verify:** order uniqueness is local to the parent, not global. Sets under different entries can both have order zero, and each appears only under its own entry in GET workout detail.

## 29. Order conflicts, flush and transaction rollback

Sources: WorkoutService add/update methods, V4 unique constraints and `preservesOrderingAndRollsBackDuplicatePositions` in WorkoutApiIntegrationTest.

New logged exercises append at max display order + 1; sets append similarly unless the creation request specifies an order. Sibling positions are unique. Gaps are valid, and deletion does not renumber everything. This keeps stable order values without implementing a batch-reorder protocol before the UI exists.

A request attempting to move onto an occupied order conflicts with the database's unique constraint. Flushing surfaces that failure inside the write operation, and transaction rollback also restores any notes/performance changed by the same request. Concurrent append requests can compute the same next order; one can receive 409 and must reload/retry. We do not silently choose another position or pretend to offer optimistic versioning.

**Why it matters:** transaction correctness includes failed requests, not just successful inserts. A conflict should not save half the edit or overwrite another entry's position.

**Exercise:** create two logged entries in a disposable workout and try moving the second onto the first's position while changing notes. Then move it to an unused position.

**Verify:** the occupied move returns 409 and the original order/notes remain. The unused move succeeds and workout detail is sorted accordingly. Atomic swapping/reorder-all is not implemented.

## 30. Testing migrations and deletion with real PostgreSQL

Sources: `src/test/java/com/punttiurpo/CatalogueMigrationIntegrationTest.java`, `WorkoutApiIntegrationTest.java` and `service/WorkoutServiceTest.java` under `backend/`.

The migration test first applies only V1/V2, inserts a custom record, captures an equivalent built-in ID, then upgrades to the latest migrations. It checks the exact ten-name set, unchanged custom data/ID and no duplicates on another migrate call. This verifies an upgrade path, not just fresh installation.

Workout integration tests run full HTTP requests backed by PostgreSQL and inspect rows after transaction boundaries. They verify nested persistence, decimal precision, draft/completed boundaries, time validation, unique-order rollback, deletion isolation and restricted catalogue references. Direct SQL checks prove integrity does not depend only on controller validation.

Unit tests check cross-field rules before repository calls. They do not claim to test SQL. Each integration class owns a disposable database; cleanup deletes only records in that test database. No test connects to the development volume or uses personal workout history.

**Why it matters:** a green mock cannot prove a foreign-key cascade or migration preserves real rows. A working fresh schema also cannot prove upgrades from earlier milestones are safe.

**Exercise:** run `./mvnw -Dtest=WorkoutServiceTest test`, then `./mvnw verify` with Docker running.

**Verify:** three focused workout unit tests pass; current full verification runs 67 tests across eight classes (44 unit/parameterized, 23 database/integration). The packaged JAR can then be started using the README setup.

## Milestone 3 review questions and exercise

1. Why do WorkoutExercise and WorkoutSet reference their parents, and what prevents deleting a catalogue exercise that is still logged?
2. What is the difference between PostgreSQL ON DELETE CASCADE and JPA cascade/orphan-removal behavior in our code?
3. Why do set inputs use BigDecimal and a nullable completion input, while persisted completion is a non-null boolean?

Suggested exercise: create a disposable workout through Swagger, add two requested exercises, prepare a draft set, complete it, inspect the nested detail and provoke an occupied-order conflict. Remove only your disposable workout and verify catalogue exercises remain. This completed the Milestone 3 review. Milestone 4 was subsequently approved and its RIR/e1RM implementation is explained in the following chapters.

# Milestone 4: pure calculations and honest estimates

## 31. A pure domain function instead of another persistence service

Sources: `backend/src/main/java/com/punttiurpo/domain/TrainingCalculations.java`, `domain/SetEstimates.java` and `mapper/WorkoutMapper.java`.

A pure calculation returns the same output for the same inputs without changing outside state. TrainingCalculations receives scalar kg, reps, RPE and completion/type flags. It does not load an entity, run SQL, call Spring, read the clock or contact a web service. A private constructor prevents treating this utility as a stateful object; static methods provide the operations directly. This resembles a small stateless TypeScript module or explicit PHP calculation helper.

WorkoutMapper calls the function whenever it creates a WorkoutSetResponse. The same mapper already serves writes, single reads and nested reads, so one calculation path covers all of them. An update cannot leave a stale stored estimate because no estimate is stored. No new controller, entity or migration was needed.

**Why it matters:** separating arithmetic from persistence makes both the meaning and test surface smaller. It also avoids duplicating a formula across endpoints.

**Exercise:** trace one POST set and one GET nested workout to `WorkoutMapper.set`, then identify where the calculator is called.

**Verify:** both paths use the same function. Search the entity and migrations: no estimated-RIR/e1RM column was added. The API result changes when raw set values change, not through a background backfill.

## 32. Exact decimal arithmetic and the rounding boundary

Source: TrainingCalculations and `src/test/java/com/punttiurpo/domain/TrainingCalculationsTest.java` under `backend/`.

The approved Epley formula is `weight × (1 + reps / 30)`. Algebraically we implement `weight × (30 + reps) / 30` to avoid rounding the division before multiplying.

```java
estimate = weightKg.multiply(THIRTY.add(BigDecimal.valueOf(repetitions)))
        .divide(THIRTY, 2, RoundingMode.HALF_UP);
```

BigDecimal has no overloaded arithmetic operators; its methods return new values. Repeating decimal division needs an explicit scale/rounding rule. We choose two final kg decimals and HALF_UP, rounding a halfway value upward for positive inputs. There is no intermediate `double`, and no approximate decimal constant like 0.033 replaces 1/30.

For example, 100 kg × 10 gives 133.333… → 133.33. A test of 0.15 × (33/30) yields 0.165 → 0.17. Two display decimals do not claim the physiological prediction is accurate to 0.01 kg. A single repetition uses the recorded load as an explicit product convention, accompanied by a warning that it is not a verified maximum.

**Why it matters:** rounding too early can change results. A chosen precision/rounding rule is part of the contract and should be testable independently of JSON/database formatting.

**Exercise:** run `./mvnw -Dtest=TrainingCalculationsTest test` and find the test covering 100.01 kg × 2 reps.

**Verify:** the result is 106.68 kg and the stored BigDecimal output scale is two. The tests also cover the one-rep special case and maximum supported input load; no Docker is required for these calculation unit tests.

## 33. Optional, nullable output and explicit unavailable reasons

Sources: TrainingCalculations and SetEstimates.

`rpeToEstimatedRir` returns `Optional<BigDecimal>`. An absent RPE yields Optional.empty; valid input yields `10 − RPE`, with one decimal place. An invalid RPE at this pure method's boundary throws IllegalArgumentException. The HTTP layer already validates the supported range/half-steps; the function also protects its direct callers.

The result record uses nullable numeric components to represent unavailable values in JSON. This is an explicit boundary choice, not a claim that null is preferable everywhere. `estimatedRir` can be null independently of e1RM. The E1rmStatus enum explains why e1RM is unavailable; zero is never used as an unknown-strength placeholder.

Gates are ordered: draft → not working set → missing/nonpositive load → unsupported reps. A completed warmup with RPE may have estimated RIR but no e1RM. A completed set of 12 reps remains a valid log but returns null e1RM with UNSUPPORTED_REPETITIONS. AVAILABLE means the inputs passed product policy, not that an exercise/person has been scientifically validated for prediction.

**Why it matters:** zero RIR means approximately no repetitions in reserve, whereas null means unrecorded/unavailable. Consumers must not confuse those states or plot null e1RM as zero strength.

**Exercise:** create completed sets at RPE 10, with no RPE, and with 11 reps. Inspect each estimates object.

**Verify:** RPE 10 maps to estimatedRir 0.0; missing RPE maps to null; an 11-rep completed set can retain RIR but has null e1RM with an explicit status. A draft returns both numeric estimates null.

## 34. Immutable result records and visible uncertainty

Source: SetEstimates and its nested E1rmStatus/Warning enums.

The record is immutable at the component-reference level. A normal list could still be mutated through that reference, so the compact constructor copies warnings using `List.copyOf`. The result cannot accidentally gain another warning because a caller modified the original ArrayList after calculation.

The calculator adds effort warnings instead of changing the Epley arithmetic using subjective RIR. At the same kg/reps, RPE 8.5 and RPE 10 produce the same repetition-based e1RM; estimated RIR and warnings differ. Missing effort is not inferred to mean maximal effort. A single always adds SINGLE_REPETITION_NOT_VERIFIED_MAXIMUM.

These enum codes are suitable for stable API behavior; the Milestone 5 frontend renders understandable Finnish text and the label “Toistomäärään perustuva e1RM-arvio (ei RPE-korjattu)”. The current Swagger schema explains the meaning. General exercise/equipment limitations still apply even when the warning list is empty.

**Why it matters:** correct arithmetic can still communicate the wrong claim. A subjective rating, prediction and measured personal record are different kinds of information.

**Exercise:** record 100 kg × 6 at RPE 8.5, then replace only the RPE with 10 while resending the full set body.

**Verify:** both repetition-based values are 120.00 kg. RIR changes 1.5 → 0.0 and the submaximal warning disappears; this does not create a measured record. Review CALCULATIONS.md for the separately researched, unimplemented RPE-aware candidate.

## 35. Parameterized tests and physiological limits

Sources: TrainingCalculationsTest and the two new calculation cases in `WorkoutApiIntegrationTest.java`.

JUnit parameterized tests run one test method with multiple explicit inputs. `@CsvSource` covers every supported RPE half-step and known Epley/rounding values; `@ValueSource` tests invalid ratings; `@MethodSource` supplies varied nullable/boolean inputs and expected eligibility statuses. This expresses boundaries compactly without a generic testing framework of our own.

HTTP/PostgreSQL tests verify that actual JSON responses agree across POST, PUT, GET set and nested reads, that high reps/drafts become unavailable correctly and that missing/submaximal effort remains visible. Pure arithmetic tests do not prove a physiological model is valid. The research note explicitly distinguishes published RIR-scale evidence from the unvalidated inference of adding RIR to Epley reps.

**Why it matters:** tests establish implemented behavior, not scientific truth. Combining unit cases with a few real response tests proves both policy and integration without requiring Docker for every arithmetic example.

**Exercise:** run the focused calculation unit test, then full `./mvnw verify` with Docker running. Inspect a parameterized case where the final division repeats.

**Verify:** 36 calculation cases pass independently; full verification now runs 67 tests across eight classes (44 unit/parameterized, 23 PostgreSQL/integration). The database schema remains four migrations, and original observations remain unchanged.

## Milestone 4 review questions and exercise

1. Why multiply before division and round only the final kg result?
2. How do null RIR, zero RIR and an unavailable e1RM express different information?
3. Why does changing RPE change warnings/RIR but not the current repetition-based Epley value?

Suggested exercise: in a disposable workout compare a completed 100 kg × 6 set at RPE 8.5 and 10, a completed 11-rep set, and a draft. Explain the estimates/status/warnings from the pure function, then remove only your disposable workout. No RPE-adjusted formula or frontend is included; stop before Milestone 5 until approved.

## 36. Following a React save into Java

Milestone 5 is now implemented. The earlier Milestone 4 stop above records that milestone's review boundary; the user subsequently approved the frontend. No new Java production class was needed: the UI exercises the same controllers, DTOs, services, mapper and calculation functions you already studied.

Start with `frontend/src/components/SetRow.tsx`: the editable `SetDraft` holds strings so a comma decimal or an incomplete field can remain visible while typing. `frontend/src/utils.ts` validates and normalizes them into `SetInput`; `frontend/src/api.ts` serializes JSON and sends PUT. This is comparable to a TypeScript form calling a PHP REST endpoint, except the server target is a Java process.

Follow the request into `backend/src/main/java/com/punttiurpo/dto/WorkoutSetRequest.java`, then `service/WorkoutService.java` and `mapper/WorkoutMapper.java` under the same package root. The request record and Bean Validation enforce server rules; the service loads/updates the managed entity inside a transaction; the mapper returns a response record containing estimates from `domain/TrainingCalculations.java`. React renders that response. The request and response are DTOs, not shared objects across the network.

**Why it matters:** React's TypeScript types disappear after compilation. They cannot prevent another HTTP client from sending invalid JSON. Java validation and SQL constraints protect all clients. React is also not a second place to maintain the Epley formula.

**Exercise:** use browser Network tools to inspect one completed-set PUT at 82.25 kg × 6 and RPE 8.5. Find the matching request fields and response estimates in the Java records.

**Verify:** JSON uses numeric 82.25 even when the input displayed `82,25`; the server returns RIR 1.5, repetition-based e1RM 98.70 and its submaximal-effort warning. Changing RPE affects effort/warnings, not the current Epley result. No formula should appear in the TypeScript input helper.

## 37. Build time, runtime and exact numbers at the boundary

`frontend/package.json` and `package-lock.json` manage the browser dependency graph; `backend/pom.xml` and the Maven Wrapper manage the Java graph. `npm run build` checks TypeScript and bundles browser JavaScript/CSS into `frontend/dist/`. It produces neither JVM bytecode nor an executable JAR. `./mvnw verify` compiles Java, tests it and packages the server JAR. Both builds can pass independently while the other process is stopped.

`frontend/vite.config.ts` forwards local `/api` requests to Java, not directly to PostgreSQL. The browser sends JSON numbers; Java's configured deserializer binds decimal values to BigDecimal, and PostgreSQL stores numeric columns. JavaScript Number is used to prepare transport values, never for cumulative strength calculations or exact persistence. `utils.ts` rejects over-precision before sending, and the backend independently validates it.

**Why it matters:** a TypeScript interface resembles the shape of a Java record, but it does not provide constructors, Bean Validation or runtime numeric precision. A successful npm build does not prove Java startup, database connectivity or migration correctness.

**Exercise:** run `npm run check` and `npm run build` from `frontend/`, then inspect a set DTO and `TrainingCalculations` in Java. Explain which system rejects three kg decimals and which calculates/rounds e1RM.

**Verify:** frontend checks pass, `dist/` is generated and ignored; a real API request with invalid precision is rejected independently of the UI. Do not modify applied migrations or reset the development volume for this exercise.

## 38. Form state is not a transaction

In `frontend/src/components/WorkoutEditor.tsx`, adding an exercise and its first draft set makes two requests. Each request reaches its own transactional Java service method. A transaction around one request cannot roll back an already committed earlier request if the next request fails. This resembles two independent PHP HTTP calls, not one SQL transaction shared through a browser.

The UI therefore keeps a successfully created exercise visible if creating its first set fails. It does not retry blindly. Likewise, a network timeout may happen after Java committed the write. `api.ts` explicitly avoids automatic mutation retries, and `hooks.ts` ignores obsolete GET responses. These are client reliability rules, not replacements for `@Transactional`.

**Why it matters:** local React draft state, a successful HTTP response and a committed database row are three distinct facts. Unsaved-navigation prompts help prevent accidental loss, but are not persistence or backups. Concurrent tabs still have no optimistic version check.

**Exercise:** read the frontend test named “keeps the persisted logged exercise visible when creating its first set fails” in `frontend/src/App.test.tsx`. Trace where the two Java requests would commit; explain why removing the entry from React would misrepresent saved data.

**Verify:** run `npm run check`; the test confirms the saved exercise remains visible with an error and an Add set recovery action. The mocked test proves UI behavior; the existing Java PostgreSQL tests prove transaction/database behavior separately. Neither establishes that Roope has already learned the concept.

## Milestone 5 review questions and exercise

1. Why must Java validate the set request even though TypeScript checks its shape?
2. Where does a set's e1RM come from, and why is there no Epley formula in React?
3. Why are two HTTP requests two transaction boundaries rather than one shared transaction?

Suggested exercise: create a disposable workout in the UI, add Bench Press and complete 82.25 kg × 6 at RPE 8.5. Add a next set, verify copied values/blank RPE, change it to 11 reps and complete it. Explain the available RIR and unavailable e1RM using the Java code. Reload to prove explicit saves persisted; delete only your disposable workout when finished. Stop before Milestone 6 until approved.


## 39. A name change is still a migration

`backend/src/main/resources/db/migration/V5__translate_builtin_exercise_names_to_finnish.sql` changes catalogue names, not exercise identity. SQL UPDATE retains primary keys, so `workout_exercise.exercise_id` still refers to the same row. The service/mapper automatically return its new Finnish name. Unlike a frontend-only translation map, this also keeps API search and Swagger consistent.

Applied V1–V4 files remain unchanged: Flyway checks their history/checksums. This is similar to adding a new PHP database migration instead of rewriting a migration that has already run on another installation. The unique-name index still applies to both custom and built-in records. A conflicting custom name causes PostgreSQL to roll the whole migration back; preserving user data is preferable to silently renaming or deleting it.

**Why it matters:** display names are editable attributes, not stable relational identity. Replacing rows can break references; renaming them in place preserves the graph.

**Exercise:** read `CatalogueMigrationIntegrationTest` and trace the stored bench ID from the original Finnish seed through the English V3 names to Finnish V5. Identify the separate conflict test.

**Verify:** `./mvnw verify` checks unchanged ID/reference, existing workout/set performance and custom data. The conflict case leaves every catalogue row unchanged. Do not edit applied migrations or manufacture a conflict in your everyday database.


## 40. Authentication, authorization and the Spring filter chain

`backend/src/main/java/com/punttiurpo/security/SecurityConfig.java` creates `@Bean` objects for a BCrypt encoder, two configured users and a `SecurityFilterChain`. Authentication establishes who is logged in; authorization decides which operations that principal may perform. A username alone proves neither. Owner has write rights; demo has read-only rights. Shared role checks run before controller methods, including when someone calls the API directly instead of using React.

Spring's form-login filter checks the password with an adaptive hash, manages session fixation protection and stores authentication in a server-side session. The browser receives an HttpOnly cookie, not a Java object or plaintext password. `SessionController` supplies a masked CSRF token to React, which sends it back for unsafe requests. CSRF protects cookie-authenticated requests from being initiated by another site; it does not replace account/record authorization. Familiar PHP session authentication has the same broad separation of cookie identifier and server session data.

**Why it matters:** hiding a button is a UX decision. The security filter and repository rules protect data even when UI checks are bypassed. Sessions/CSRF are configured through framework APIs rather than custom credential/token code.

**Exercise:** read the matcher order in SecurityConfig. Predict the responses for anonymous catalogue GET, demo workout POST with valid CSRF, owner POST without CSRF and owner Swagger GET.

**Verify:** the HTTP security tests assert 401, 403, 403 and owner access respectively. Existing HTTP tests now log in through actual cookies/CSRF using `AuthenticatedClient`; they do not disable the filter chain.

## 41. The authenticated principal defines record scope

`security/CurrentAccount.java` reads Spring's current authenticated principal. `service/WorkoutService.java` passes its account key into scoped repository queries for lists, workouts, logged exercises and sets. For a set, the query follows `set → logged exercise → workout → ownerKey`. This is relationship traversal in Spring Data's derived query method name, comparable to a SQL JOIN with an ownership WHERE clause. Response children are fetched only after the parent has passed ownership validation.

The client cannot choose ownership in `WorkoutRequest`: the service assigns it from CurrentAccount. `ExerciseRepository.findVisible` combines shared built-ins with the owner's custom rows. V6 adds/backfills owner keys without changing existing IDs or performance. The two account keys are fixed by configuration/check constraints; there is no general registration/user entity in this deliberately small version.

**Why it matters:** logging in is not enough if `/api/sets/123` returns anyone's set. Scoped lookups return the same 404 for missing and foreign records. The owner is not an administrator of the demo dataset.

**Exercise:** trace one foreign set GET through security filters, controller, service and repository. Explain why demo receives 404 for the owner's set but 403 for any attempted mutation.

**Verify:** `SecurityIntegrationTest` creates an owner workout/entry/set, checks both account directions, hidden custom exercise searches and an attempted owner-key spoof. Migration tests verify existing records belong to owner and only invented seed records belong to demo.

## 42. Configuration and credentials are different kinds of state

`SecurityConfig.accounts` requires two BCrypt hashes from environment configuration. `backend/tools/ProvisionAccounts.java` generates random local passwords, stores hashes in ignored `.env` and writes a 0600 ignored `.env.accounts` handoff. It is a standalone Java source-launcher utility, not part of application startup or a Flyway migration. `HashPassword.java` provides an interactive no-echo hashing command for deliberate rotation. No registration or email recovery endpoint exists.

Passwords are long-term secrets; sessions are temporary credentials; recorded workouts are domain data. Restarting Java invalidates process-local sessions without deleting workouts. Updating an environment file does not mutate an already running Java process, so rotation requires re-exporting/restarting. Unlike browser TypeScript build variables, backend environment variables do not automatically become public JavaScript. Neither secrets nor private backups belong in Git.

**Exercise:** inspect `.env.example`, the two tools and AUTHENTICATION.md without printing your real credentials. Identify where each category of state is stored.

**Verify:** Java refuses to start without valid distinct hashes; Git ignores credential handoff/configuration and the pre-auth local backup. The production Secure-cookie default is true, and only local HTTP configuration overrides it. Do not rotate passwords merely to complete this reading exercise.

## Two-account review questions and exercise

1. How do authentication, role authorization and record ownership checks differ?
2. Why does the service use the authenticated account rather than a JSON owner key?
3. Why must React obtain a new CSRF token after login, and why should a failed write not be replayed automatically?

Exercise: in separate normal/private browser sessions, log in as owner and demo. Create one disposable owner workout and try opening its URL as demo. Explain the 404, the separate demo list, and the disabled demo controls using the Java code. Remove only your disposable owner workout. Understanding has not yet been demonstrated; this is a review exercise after implementation.

## 43. Build with a JDK, run with a runtime image

`deploy/backend.Dockerfile` has separate build and runtime stages. The build stage uses Temurin JDK 25 to run the pinned Maven Wrapper and compile/package Java. The runtime stage copies only the executable Spring Boot JAR and a tiny health-check class into a Temurin runtime image. This is similar to compiling TypeScript with development tools and serving the resulting JavaScript, except the final application still runs on the JVM. The runtime image does not contain Maven or project tests.

`deploy/HealthCheck.java` is a standalone Java class with `main`, using JDK networking APIs and no Spring dependency. The build stage compiles it with `javac -d /health`; Docker executes it with `java -cp /health HealthCheck`. It calls internal aggregate health and exits nonzero if unhealthy. The app itself still starts with `java -jar /app/app.jar`; these are two conventional JVM entry mechanisms. The short-lived health JVM is separate from the long-running application process.

**Why it matters:** a healthy container process alone cannot prove dependency readiness. The Java health helper checks the app/database result before the web service is considered ready. Container memory includes JVM overhead, threads and the short health process, not only the Java heap.

**Exercise:** compare the two Java commands in the Dockerfile/Compose file and explain how each locates its main class. Identify the build tools absent from the final stage.

**Verify:** native AMD64 runtime reports Temurin 25.0.4.1; Compose health reaches healthy through the compiled helper. A disposable real set survived restarting only the dedicated production database. Build wrappers retain checksum validation: the minimal image needed unzip to avoid selecting tar.gz against the pinned ZIP checksum. No checksum was disabled.

## Deployment review questions and exercise

1. Why does compiling Java on AMD64 not require changing the application package or source code?
2. How do `java -jar` and `java -cp … HealthCheck` find their entry points?
3. Why is JVM heap size smaller than the container's total memory limit?

Exercise: read `deploy/HealthCheck.java` and trace what happens if Java is alive but its database is unavailable. Use the existing disposable Testcontainers outage test locally rather than stopping the production database for practice. Understanding has not yet been demonstrated; deployment verification is not a substitute for explaining the code.


## 44. Publishing UI copy does not rebuild Java

`deploy/publish` is a Bash operations helper, not a Spring service. React copy lives in source files but nginx serves compiled assets inside an image. The helper runs frontend checks, builds that image and recreates only the web container. `deploy/compose.yaml` uses optional WEB_RELEASE for the frontend image, falling back to RELEASE; Java keeps its existing image and process-local sessions. This resembles compiling TypeScript and publishing the generated bundle while keeping a PHP/API service running. Git push stores source; it does not execute this helper.

The script compares committed backend/infrastructure files against the running images' commit tags. Backend changes need the full release procedure, including migration review and backups. The helper's public checks prove basic delivery and anonymous API denial; they do not prove every logged-in flow or that the wording is correct.

**Exercise:** after committing a small UI copy edit, use `./deploy/publish --dry-run` and explain why it can identify a release without uploading anything. Before actual publication, identify which Compose service will be recreated and why Java does not need to restart. See deploy/README.md for the actual publication command.

**Verify:** `python3 deploy/test-publish.py` checks rejection of dirty/backend/infrastructure changes, successful web-only activation, build failure, verification-triggered rollback and a concurrent production change using fake external commands. These are deployment control-flow tests; Java/PostgreSQL behavior remains covered by the existing backend tests.
