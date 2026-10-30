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

# Extract Spring Boot layers for faster classloading and optimized layer caching
RUN java -Djarmode=layertools -jar target/mobility-wallet-1.0.0-SNAPSHOT.jar extract

# ------------------------------------------------------------------------------
# Stage 2: Minimal Slim Runtime Stage (Optimized for Fast Cold Starts)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create unprivileged system group and user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy pre-extracted layers with correct ownership (eliminates slow chown -R step)
COPY --from=builder --chown=appuser:appgroup /build/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /build/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/application/ ./

# Switch to non-root execution
USER appuser

# Expose default HTTP service port
EXPOSE 8080

# Production JVM flags engineered for micro/free-tier cloud containers (512MB RAM, throttled vCPU):
# 1. -XX:TieredStopAtLevel=1: Restricts JIT to C1 compiler, cutting startup compilation CPU by ~60%
# 2. -XX:+UseSerialGC: Eliminates multi-threaded GC thread overhead on single-core containers
# 3. -Dspring.jmx.enabled=false: Skips JMX bean registration overhead
# 4. -Djava.security.egd=file:/dev/./urandom: Fast non-blocking entropy for SSL and BCrypt
ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Dspring.jmx.enabled=false -Djava.security.egd=file:/dev/./urandom"

# Cloud host health check probing Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Launch application via Spring Boot 3 LayerLauncher (fast direct classloading)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
