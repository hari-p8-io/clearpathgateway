# Fast Liquidity Service Dockerfile
# Multi-stage build for optimized production image

# Build stage
FROM maven:3.9-openjdk-21-slim AS build

WORKDIR /app

# Copy parent POM and BOM first for better layer caching
COPY pom.xml .
COPY fast-cpg-bom/ ./fast-cpg-bom/

# Copy service source
COPY services/fast-liquidity-service/ ./services/fast-liquidity-service/

# Build the application
RUN mvn clean package -pl services/fast-liquidity-service -am -DskipTests

# Runtime stage
FROM openjdk:21-jre-slim

# Install security updates and useful tools
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat-traditional \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN groupadd -r liquidity && useradd -r -g liquidity liquidity

# Set working directory
WORKDIR /app

# Copy application JAR
COPY --from=build /app/services/fast-liquidity-service/target/fast-liquidity-service-*.jar app.jar

# Change ownership to application user
RUN chown -R liquidity:liquidity /app

# Switch to application user
USER liquidity

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8084/health || exit 1

# Expose port
EXPOSE 8084

# JVM configuration for production
ENV JAVA_OPTS="-XX:+UseZGC \
               -XX:+UseVirtualThreads \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+UseContainerSupport \
               -Djava.security.egd=file:/dev/./urandom"

# Application entrypoint
ENTRYPOINT ["java", \
           "--enable-preview", \
           "-XX:+UseZGC", \
           "-XX:+UseVirtualThreads", \
           "-jar", "app.jar"]

# Labels for metadata
LABEL maintainer="APEAFAST-SG Team <apeafast-sg@anz.com>"
LABEL version="21.0.0-apeafast-SNAPSHOT"
LABEL description="Fast Liquidity Service - Multi-region liquidity management for APEAFAST-SG payment gateway"
LABEL service="fast-liquidity-service"
LABEL tier="backend"