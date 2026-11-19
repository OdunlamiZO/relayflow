# syntax=docker/dockerfile:1
# Built from the repository root so the plugin API and the official plugins are in the context.
# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY packages/plugin-api packages/plugin-api
COPY plugins plugins
COPY apps/api/pom.xml apps/api/pom.xml
COPY apps/api/src apps/api/src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -DskipTests package -pl apps/api,plugins/anthropic,plugins/openai-compatible,plugins/openai-compatible-transcription,plugins/local-disk-storage -am

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
 && addgroup -S relayflow && adduser -S relayflow -G relayflow \
 && mkdir -p /app/plugins/official /app/plugins/custom /app/data/storage \
 && chown -R relayflow:relayflow /app/data
WORKDIR /app
COPY --from=build /build/apps/api/target/relayflow-api-*.jar app.jar
COPY --from=build /build/plugins/*/target/relayflow-plugin-*.jar /app/plugins/official/
ENV RELAYFLOW_PLUGINS_DIRECTORY=/app/plugins \
    RELAYFLOW_STORAGE_DIRECTORY=/app/data/storage
USER relayflow
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=45s --retries=5 \
  CMD curl -fs http://127.0.0.1:8080/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
