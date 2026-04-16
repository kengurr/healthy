# Multi-stage build for Zdravdom Backend
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies (layer caching)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -S zdravdom && adduser -S zdravdom -G zdravdom
USER zdravdom:zdravdom

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Copy configuration (should be mounted as volume in production)
COPY src/main/resources/application.yml config/

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-standalone -qO- http://localhost:8080/api/v1/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=file:./config/"]