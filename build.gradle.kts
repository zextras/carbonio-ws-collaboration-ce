// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

val revision = "1.13.9"
val isProd = providers.gradleProperty("prod").isPresent

allprojects {
    group = "com.zextras.carbonio.ws-collaboration"
    version = revision + if (isProd) "" else "-SNAPSHOT"
}

subprojects {
    plugins.withType<JavaPlugin>().configureEach {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        dependencies {
            "testRuntimeOnly"(libs.junit.jupiter.engine)
            "testRuntimeOnly"(libs.junit.platform.launcher)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }
}
