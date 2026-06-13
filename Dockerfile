# Multi-stage build: produce a slim JRE runtime image from a Maven-cached build.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy the Maven wrapper + pom.xml first so dependency resolution is cached
# across rebuilds when only source changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src ./src
RUN ./mvnw -B -ntp clean package -DskipTests \
    && cp target/email-messenger-*.jar /workspace/app.jar


FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl is used by the HEALTHCHECK below; remove apt cache to keep the layer slim.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Non-root runtime user.
RUN groupadd --system --gid 1000 app \
    && useradd  --system --uid 1000 --gid app --shell /usr/sbin/nologin app

COPY --from=build /workspace/app.jar /app/app.jar
RUN chown app:app /app/app.jar

USER app:app

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Start-period gives the JVM + Flyway time to come up before the first probe
# counts toward failure. /actuator/health returns 200 once the DataSource is
# reachable, which is the signal every platform LB needs to route traffic.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
