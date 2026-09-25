// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    `java-library`
    alias(libs.plugins.openapi.generator)
}

dependencies {
    api(libs.swagger.annotations)
    api(libs.jsr305)
    api(libs.resteasy.client)
    api(libs.resteasy.multipartProvider)
    api(libs.jackson.core)
    api(libs.jackson.annotations)
    api(libs.jackson.databind)
    api(libs.jackson.databindNullable)
    api(libs.resteasy.jackson2Provider)
    api(libs.jetty.servlet)
    api(libs.guice)
    api(libs.swagger.jaxrs)
}

val resourcesDir = layout.projectDirectory.dir("src/main/resources")
val apiGenDir = layout.buildDirectory.dir("generated-sources/chats")
val asyncGenDir = layout.buildDirectory.dir("generated-sources/async")

val generateApi by tasks.registering(GenerateTask::class) {
    generatorName.set("jaxrs-resteasy")
    inputSpec.set(resourcesDir.file("api.yaml").asFile.absolutePath)
    configFile.set(resourcesDir.file("generator-config.json").asFile.absolutePath)
    templateDir.set(resourcesDir.dir("templates").asFile.absolutePath)
    ignoreFileOverride.set(resourcesDir.file("generator-ignore").asFile.absolutePath)
    outputDir.set(apiGenDir)
    modelNameSuffix.set("dto")
    skipValidateSpec.set(true)
    configOptions.set(mapOf("useJakartaEe" to "true"))
    typeMappings.set(mapOf("binary" to "InputStream", "file" to "InputStream"))
    importMappings.set(mapOf("InputStream" to "java.io.InputStream"))
}

val generateAsyncApi by tasks.registering(GenerateTask::class) {
    generatorName.set("jaxrs-resteasy")
    inputSpec.set(resourcesDir.file("asyncapi.yaml").asFile.absolutePath)
    configFile.set(resourcesDir.file("async-generator-config.json").asFile.absolutePath)
    templateDir.set(resourcesDir.dir("templates").asFile.absolutePath)
    ignoreFileOverride.set(resourcesDir.file("generator-ignore").asFile.absolutePath)
    outputDir.set(asyncGenDir)
    skipValidateSpec.set(true)
    generateModelTests.set(false)
    generateApiTests.set(false)
    generateModelDocumentation.set(false)
    generateApiDocumentation.set(false)
    globalProperties.set(
        mapOf(
            "models" to "",
            "apis" to "false",
            "supportingFiles" to "false",
        ),
    )
    configOptions.set(
        mapOf(
            "useJakartaEe" to "true",
            "jsonTypeInfoPropertyName" to "type",
            "jsonTypeInfoAs" to "EXISTING_PROPERTY",
        ),
    )
}

// OpenAPI Generator writes Java sources under each output directory's src/main/java.
sourceSets {
    main {
        java {
            srcDir(apiGenDir.map { it.dir("src/main/java") })
            srcDir(asyncGenDir.map { it.dir("src/main/java") })
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateApi, generateAsyncApi)
}
