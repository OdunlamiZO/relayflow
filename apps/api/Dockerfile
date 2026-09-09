# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
 && addgroup -S relayflow && adduser -S relayflow -G relayflow
WORKDIR /app
COPY --from=build /build/target/api-*.jar app.jar
USER relayflow
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=45s --retries=5 \
  CMD curl -fs http://127.0.0.1:8080/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
