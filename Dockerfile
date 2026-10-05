# ==============================================================================
# Multi-Stage Dockerfile for Mobility Wallet Platform
# Stage 1: Build & Package (Maven + Eclipse Temurin JDK 17 Alpine)
# Stage 2: Runtime (Eclipse Temurin JRE 17 Alpine - Lightweight & Hardened)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Maven Build Stage
# ------------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Package production executable JAR without running test suites inside container build
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Slim Runtime Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create unprivileged system group and user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled executable JAR from builder stage
COPY --from=builder /build/target/mobility-wallet-1.0.0-SNAPSHOT.jar app.jar

# Enforce non-root file ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root execution
USER appuser

# Expose default HTTP service port
EXPOSE 8080

# Production container default configuration
ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Cloud host health check probing Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Launch application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
