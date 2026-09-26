# syntax=docker/dockerfile:1

# ── Build ─────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle lint.gradle checkstyle.xml pmd-ruleset.xml spotbugs-filter.xml ./
COPY gradle gradle
COPY src src

# Gradle's own cache lives in a BuildKit cache mount, so the distribution and
# the dependencies are downloaded once and survive every later rebuild.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar \
    && cp "$(ls build/libs/*.jar | grep -v -- '-plain\.jar$')" app.jar

# ── Run ───────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --uid 10001 --no-create-home codeforge
COPY --from=build /workspace/app.jar app.jar
USER codeforge

EXPOSE 8080

# A share of the container's memory limit rather than the JVM's default quarter
# of it: this container runs nothing else.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"

# The image has neither curl nor wget, so the request is made with bash alone.
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 \
        && printf "GET /actuator/health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n" >&3 \
        && grep -q "\"status\":\"UP\"" <&3'

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
