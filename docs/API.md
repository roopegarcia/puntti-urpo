# HTTP API

## GET /actuator/health

Implemented using Spring Boot Actuator. No request body, parameters or authentication are required for this local operational endpoint.

```sh
curl -i http://127.0.0.1:8080/actuator/health
```

Healthy dependencies: HTTP **200**, JSON body:

```json
{"status":"UP"}
```

A required dependency unavailable after application startup: HTTP **503**, JSON body:

```json
{"status":"DOWN"}
```

Component names, connection details and exceptions are hidden. Liveness/readiness probe groups are disabled. Only the health Actuator endpoint is exposed over HTTP; endpoints such as `env` and `configprops` are not enabled for web exposure. Health describes the aggregate checks, including database connectivity and disk space, not just whether the JVM exists.

Flyway needs the database at startup. If initial database connection or migration validation fails, startup fails: there is no running HTTP server to return a 503 health response. The 503 contract describes dependency failure after successful startup.

## Other routes

`/` returns HTTP 404 because no product route exists. Default error responses exclude internal messages, binding details and stack traces. Internal diagnostic logs may still contain exception details and must remain private.

No exercise/workout APIs, DTOs, validation rules, OpenAPI UI or authentication exist yet. Endpoints in `AGENTS.md` remain suggestions rather than final contracts. Add request parameters, DTOs, validation, examples and consistent business error responses as those APIs are implemented. Authentication and authorization must precede public access to personal data or write operations.
