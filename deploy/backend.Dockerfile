FROM eclipse-temurin:25-jdk@sha256:2d5d5e9b533342a565907c84f47a2ec298c40cde7b13a3ca85d4950fa456a641 AS build
# The script-only wrapper falls back to tar.gz without unzip, but our pinned checksum is for ZIP.
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY backend/pom.xml backend/mvnw ./
COPY backend/.mvn .mvn
RUN chmod +x mvnw
COPY backend/src/main src/main
RUN ./mvnw -B -ntp -Dmaven.test.skip=true package
COPY deploy/HealthCheck.java /health/HealthCheck.java
RUN javac -d /health /health/HealthCheck.java

FROM eclipse-temurin:25-jre@sha256:dbb43d81099eec91e0800226ed2296d03146ee7857165b95112372ad0d67e87f
WORKDIR /app
COPY --from=build /build/target/puntti-urpo-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /health/HealthCheck.class /health/HealthCheck.class
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
