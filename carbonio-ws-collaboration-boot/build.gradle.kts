// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

plugins {
    java
    alias(libs.plugins.shadow)
}

val mainClassName = "com.zextras.carbonio.chats.boot.Chats"

dependencies {
    implementation(libs.systemdNotify)
    implementation(project(":carbonio-ws-collaboration-core")) {
        exclude(group = "jakarta.ws.rs", module = "jsr311-api")
    }

    implementation(libs.jetty.server)
    implementation(libs.jetty.servlet)
    implementation(libs.jetty.websocket)
    implementation(libs.jakarta.el)
    implementation(libs.guice)
    implementation(libs.resteasy.guice)
    implementation(libs.resteasy.servletInitializer)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}

tasks.compileJava {
    options.compilerArgs.add("--enable-preview")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = mainClassName
    }
}

tasks.shadowJar {
    archiveFileName.set("carbonio-ws-collaboration-ce-${project.version}-fatjar.jar")
    manifest {
        attributes["Main-Class"] = mainClassName
    }
    mergeServiceFiles()
    exclude(
        "module-info.class",
        "META-INF/*.MF",
        "META-INF/*.md",
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/NOTICE",
    )
}

tasks.build {
    dependsOn(tasks.shadowJar, tasks.named("copyDependencies"))
}

tasks.register<Sync>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("lib"))
}
