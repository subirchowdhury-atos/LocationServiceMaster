# Multi-stage build for optimized image size
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy only pom.xml first for better layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build with proper options
RUN mvn clean package -DskipTests -Dspring-boot.build-image.skip=true

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Add non-root user
RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring

# Install curl for health check
RUN apk add --no-cache curl

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/location-service-master.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

# Better health check path
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# JVM options optimized for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run with proper JVM options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]