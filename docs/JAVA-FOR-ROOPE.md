# Java for Roope

This tutorial grows with Puntti-Urpo. You already know PHP, JavaScript, SQL and web development; the aim is to connect that experience to Java without hiding how the application works. Exercises are optional experiments: restore changes after trying them. Understanding has not yet been assessed. Milestone 1 is now complete; study it at your own pace before the next milestone. Sections 1–6 introduce the original startup foundation; sections 7 onward explain the database, health checks and tests added afterward. Follow the README database/environment setup before running the application examples below.

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

Source: `backend/pom.xml`, `backend/mvnw`, and `backend/.mvn/wrapper/maven-wrapper.properties`.

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

Sources: `backend/src/main/resources/application.properties`, `backend/src/main/resources/db/migration/README.md` and the `initializesFlywayWithoutCreatingDomainTables` test.

Flyway runs versioned SQL files in order and records applied migrations with checksums in `flyway_schema_history`. Think of a deliberately recorded sequence of schema changes, similar to migration tooling in PHP frameworks. This is separate from inserting workout records.

There is no domain SQL migration yet. The migration folder's README documents that choice; Flyway ignores it as SQL. Startup initializes Flyway's own history table, and the test verifies no application tables or applied/pending migrations exist. A warning about no migrations is expected at this stage.

`spring.flyway.clean-disabled=true` disables Flyway's destructive clean operation. Validation checks consistency between available and applied migrations. We will add the first actual migration with the exercise catalogue. Do not change this configuration merely to remove an expected warning.

**Why it matters:** migrations let another developer or deployment reproduce the intended schema. Allowing an ORM to change shared schemas silently would hide an important learning and operational boundary.

**Exercise:** start the local database/application using the README, then open psql using the command in `docs/DATABASE.md`. Run `SELECT count(*) FROM flyway_schema_history;` and `\dt`.

**Verify:** the history row count is zero, and the only public table is `flyway_schema_history`. Do not create a pretend exercise table to fill the empty history.

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

**Verify:** all three tests pass. This demonstrates test isolation from the Compose database. A successful test build creates the executable JAR too.

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

Suggested exercise: perform the health outage/recovery sequence above and explain each response using the source and configuration. Completing it is for your learning, not a prerequisite imposed before finishing this milestone. We still stop for your approval before implementing Milestone 2.
