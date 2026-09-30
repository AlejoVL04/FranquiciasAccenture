# ---------------------------------------------------------------------------
# Stage 1 - build
# Dependencies are resolved in their own layer so that editing sources does not
# invalidate the Maven cache on every rebuild.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Integration tests need a Docker daemon (Testcontainers), which is not
# available inside the build. They run on the host with `mvn verify`.
RUN mvn -B -DskipTests package

# ---------------------------------------------------------------------------
# Stage 2 - runtime
# JRE only, non-root user, no build tooling in the final image.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S spring && adduser -S spring -G spring \
    && apk add --no-cache wget

WORKDIR /app

COPY --from=build /build/target/franchise-api.jar app.jar

RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
