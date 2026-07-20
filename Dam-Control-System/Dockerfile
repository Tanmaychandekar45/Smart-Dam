# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer cache: only re-downloads deps when pom changes)
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and build the fat JAR (skip tests for speed)
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

# Copy only the final JAR from the build stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the Spring Boot port
EXPOSE 8080

# Environment variable defaults (overridden by docker-compose)
ENV DB_HOST=mysql \
    DB_PORT=3306 \
    DB_NAME=smart_dam_db \
    DB_USER=root \
    DB_PASSWORD=smartdam

ENTRYPOINT ["java", "-jar", "app.jar"]
