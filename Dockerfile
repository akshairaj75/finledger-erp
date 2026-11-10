# ==========================================
# Stage 1: Build JAR with Maven & OpenJDK 21
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and pom.xml first to take advantage of Docker layer caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy application source code and compile production JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Stage 2: Minimal Production JRE 21 Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="FinLedger Engineering <dev@finledger.com>"
LABEL description="FinLedger Core ERP REST API Backend (Spring Boot 3 + JRE 21)"

# Create dedicated non-root application user and directories
RUN addgroup -S finledger && adduser -S finledger -G finledger \
    && mkdir -p /app/uploads /app/logs \
    && chown -R finledger:finledger /app

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder --chown=finledger:finledger /build/target/finledger-*.jar /app/finledger-api.jar

# Run as non-root user
USER finledger

# JVM tuning for containerized workloads (G1GC, max ram 75%, out of memory crash dump)
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080

EXPOSE 8080

# Health check probe against actuator health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -q -O - http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/finledger-api.jar"]

