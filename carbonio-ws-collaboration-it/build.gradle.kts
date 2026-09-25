// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    jacoco
}

dependencies {
    testImplementation(project(":carbonio-ws-collaboration-core"))

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.mockito.core)
    testImplementation(libs.rabbitmq.mock)
    testImplementation(libs.awaitility)

    testImplementation(libs.flyway.postgres)
    testImplementation(libs.guice)
    testImplementation(libs.jetty.servlet)
    testImplementation(libs.jetty.websocket)
    testImplementation(libs.hibernate.validator.cdi)
    testImplementation(libs.jakarta.el)
    testImplementation(libs.jackson.jsr310)
}

// JDK 21+ requires an explicit Mockito agent.
val mockitoAgent: Configuration by configurations.creating
dependencies {
    mockitoAgent(libs.mockito.core) { isTransitive = false }
}

tasks.test {
    jvmArgs("-javaagent:${mockitoAgent.singleFile}")
}

val core = project(":carbonio-ws-collaboration-core")
val coreSources = core.extensions.getByType<SourceSetContainer>()["main"]

tasks.jacocoTestReport {
    dependsOn(tasks.test, core.tasks.named("classes"))
    sourceDirectories.setFrom(coreSources.allSource.srcDirs)
    classDirectories.setFrom(coreSources.output.classesDirs)
    reports {
        xml.required.set(true)
        xml.outputLocation.set(rootProject.layout.buildDirectory.file("reports/jacoco-integration-tests/jacoco.xml"))
    }
}

val jacocoAllTestsReport by tasks.registering(JacocoReport::class) {
    dependsOn(tasks.test, core.tasks.named("test"))
    sourceDirectories.setFrom(coreSources.allSource.srcDirs)
    classDirectories.setFrom(coreSources.output.classesDirs)
    executionData.setFrom(
        layout.buildDirectory.file("jacoco/test.exec"),
        core.layout.buildDirectory.file("jacoco/test.exec"),
    )
    reports {
        xml.required.set(true)
        xml.outputLocation.set(rootProject.layout.buildDirectory.file("reports/jacoco-all-tests/jacoco.xml"))
    }
}

tasks.build {
    dependsOn(tasks.jacocoTestReport, jacocoAllTestsReport)
}
