# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY ../../pom.xml .
COPY ../../carbonio-ws-collaboration-openapi/pom.xml carbonio-ws-collaboration-openapi/
COPY ../../carbonio-ws-collaboration-core/pom.xml carbonio-ws-collaboration-core/
COPY ../../carbonio-ws-collaboration-boot/pom.xml carbonio-ws-collaboration-boot/
COPY ../../carbonio-ws-collaboration-it/pom.xml carbonio-ws-collaboration-it/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY ../.. .

# Build the application
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime

RUN apt update && apt install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built artifact from the build stage
COPY --from=build /app/carbonio-ws-collaboration-boot/target/carbonio-ws-collaboration-fatjar.jar app.jar

# Create a non-root user
RUN useradd -r -u 1001 -g root appuser && \
    chown -R appuser:root /app

# Switch to non-root user
USER appuser

# Expose the application port (default Jetty port)
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Xms1024m -Xmx2048m -XX:+UseZGC" \
    TZ=UTC

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
