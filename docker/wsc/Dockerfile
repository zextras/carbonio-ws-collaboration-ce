FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17 AS local-dev

ARG TARGETPLATFORM
ARG BUILDPLATFORM

WORKDIR /app

# # Installazione di OpenSSH Server
# RUN apt-get update && \
#     apt-get install -y openssh-server && \
#     mkdir /var/run/sshd

# # Creazione utente per SSH
# RUN useradd -rm -d /home/devuser -s /bin/bash -g root -G sudo devuser && \
#     echo 'devuser:devpass' | chpasswd

# # Configurazione SSH
# RUN sed -i 's/PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
#     sed -i 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' /etc/pam.d/sshd && \
#     echo "export VISIBLE=now" >> /etc/profile

# # Espone la porta SSH
# EXPOSE 22

# # Avvia il demone SSH
# CMD ["/usr/sbin/sshd", "-D"]

FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17 AS deps

ARG TARGETPLATFORM
ARG BUILDPLATFORM

WORKDIR /app
RUN echo "Current directory is:" && pwd
COPY pom.xml .
COPY carbonio-ws-collaboration-openapi/pom.xml carbonio-ws-collaboration-openapi/
COPY carbonio-ws-collaboration-core/pom.xml carbonio-ws-collaboration-core/
COPY carbonio-ws-collaboration-boot/pom.xml carbonio-ws-collaboration-boot/
COPY carbonio-ws-collaboration-it/pom.xml carbonio-ws-collaboration-it/

# Download external dependencies
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

FROM deps AS openapi

WORKDIR /app

COPY carbonio-ws-collaboration-openapi/src carbonio-ws-collaboration-openapi/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl 'carbonio-ws-collaboration-openapi' install -DskipTests

FROM openapi AS build

WORKDIR /app


COPY carbonio-ws-collaboration-core/ carbonio-ws-collaboration-core/
COPY carbonio-ws-collaboration-boot/ carbonio-ws-collaboration-boot/
COPY carbonio-ws-collaboration-it/ carbonio-ws-collaboration-it/

# Build the application (openapi will be resolved from local repository)
RUN --mount=type=cache,target=/root/.m2 mvn -pl '!carbonio-ws-collaboration-openapi' package -DskipTests

FROM gcr.io/distroless/java17-debian12 AS final

WORKDIR /app

COPY --from=build /app/carbonio-ws-collaboration-boot/target/carbonio-ws-collaboration-ce-fatjar.jar app.jar

USER nonroot

# Expose the application port (default Jetty port) and debug port
EXPOSE 8080 5005

# Set environment variables
ENV JDK_JAVA_OPTIONS="-Djava.net.preferIPv4Stack=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Xms1024m -Xmx2048m -XX:+UseZGC" \
    TZ=UTC

ENV JDK_JAVA_OPTIONS="-Djava.net.preferIPv4Stack=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Xms1024m -Xmx2048m -XX:+UseZGC"

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
