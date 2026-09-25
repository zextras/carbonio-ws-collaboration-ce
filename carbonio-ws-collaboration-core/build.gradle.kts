// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

plugins {
    `java-library`
    alias(libs.plugins.ebean)
    jacoco
}

dependencies {
    api(project(":carbonio-ws-collaboration-openapi"))

    api(libs.userManagementRest)
    api(libs.storages)
    // storages-ce-sdk resolves artifact-only (see settings.gradle.kts); these are its POM dependencies.
    api(libs.filestore.common)
    api(libs.retrofit)
    api(libs.retrofit.gson)
    api(libs.javax.annotation)
    api(libs.previewRest)
    // carbonio-preview-ce-rest-sdk resolves artifact-only too; these are its POM dependencies.
    api(libs.httpmime)
    api(libs.swagger.annotationsJakarta)
    api(libs.vavr)

    api(libs.jetty.server)
    api(libs.jetty.servlet)
    api(libs.jetty.websocket)
    api(libs.jakarta.websocket.api)
    api(libs.jakarta.websocket.clientApi)

    api(libs.guice)
    api(libs.resteasy.guice)
    api(libs.resteasy.jackson2Provider)
    api(libs.resteasy.validatorProvider)
    api(libs.jakarta.el)

    api(libs.ebean.postgres)
    api(libs.hikari)
    api(libs.postgresql)
    api(libs.flyway.postgres)

    api(libs.slf4j.api)
    api(libs.logback.classic)

    api(libs.jackson.annotations)
    api(libs.jackson.jsr310)

    api(libs.commons.lang3)
    api(libs.commons.text)

    api(libs.consul.client)
    api(libs.amqp.client)
    api(libs.caffeine)

    api(libs.semver4j)
    api(libs.snakeyaml)

    api(libs.hibernate.validator.cdi)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockito.core)
    testImplementation(libs.ebean.test)
}

// JDK 21+ requires an explicit Mockito agent.
val mockitoAgent: Configuration by configurations.creating
dependencies {
    mockitoAgent(libs.mockito.core) { isTransitive = false }
}

tasks.test {
    jvmArgs("-javaagent:${mockitoAgent.singleFile}")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        xml.outputLocation.set(rootProject.layout.buildDirectory.file("reports/jacoco-unit-tests/jacoco.xml"))
    }
}

tasks.build {
    dependsOn(tasks.jacocoTestReport)
}
