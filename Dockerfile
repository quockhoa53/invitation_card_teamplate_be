# Multi-stage build for optimal image size and security
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy gradle wrapper and config files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Cache dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code and build jar
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser

# Copy jar from build stage
COPY --from=build --chown=appuser:appuser /app/build/libs/*.jar app.jar

EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["java", "-jar", "-Dserver.port=${PORT}", "app.jar"]
