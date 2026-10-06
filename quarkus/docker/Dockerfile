# SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
#
# SPDX-License-Identifier: AGPL-3.0-only

# Images for trying the PoC on carbonio-k3d. Build context: quarkus/ after
# `mvn -f quarkus/pom.xml package -DskipTests`.
#
#   --target app --build-arg APP=app-ce   CE (core only)
#   --target app --build-arg APP=app-adv  ADV, model A (TURN assembled at build time)
#   --target adv-b1                       ADV, model B1 (CE + recording in plugins/, re-augmented)
#
# Same base and launch contract as the Guice image: the k3d chart runs
# `java -jar /app/app.jar` and probes with /busybox/wget. app.jar is a symlink
# to the fast-jar launcher, which finds lib/ next to the real quarkus-run.jar.

FROM gcr.io/distroless/java21-debian12:debug AS base
WORKDIR /app
ENV TZ=UTC \
    JDK_JAVA_OPTIONS="-Djava.net.preferIPv4Stack=true -Xms256m -Xmx1024m -XX:+UseZGC"
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM base AS app
ARG APP=app-ce
COPY ${APP}/target/quarkus-app/ /app/quarkus-app/
RUN ["/busybox/ln", "-s", "quarkus-app/quarkus-run.jar", "/app/app.jar"]
USER nonroot

FROM base AS adv-b1
# The user-providers directory is resolved from a parent dir named quarkus-app.
COPY app-ce/target/quarkus-app/ /app/quarkus-app/
COPY plugin-recording/target/plugin-recording-*-plugin.jar /app/quarkus-app/plugins/
RUN ["java", "-Dquarkus.launch.rebuild=true", "-jar", "/app/quarkus-app/quarkus-run.jar"]
RUN ["/busybox/ln", "-s", "quarkus-app/quarkus-run.jar", "/app/app.jar"]
USER nonroot
