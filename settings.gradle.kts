// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        // These SDK POMs have broken metadata; resolve their jars directly.
        exclusiveContent {
            forRepository {
                maven {
                    name = "zextras-java-sdk-artifacts"
                    url = uri("https://zextras.jfrog.io/artifactory/java-sdk")
                    metadataSources { artifact() }
                }
            }
            filter {
                includeModule("com.zextras", "storages-ce-sdk")
                includeModule("com.zextras", "filestore-sdk-common")
                includeModule("com.zextras.carbonio.preview", "carbonio-preview-ce-rest-sdk")
            }
        }

        maven {
            name = "zextras-java-sdk"
            url = uri("https://zextras.jfrog.io/artifactory/java-sdk")
        }
        mavenCentral()
    }
}

buildCache {
    local {
        isEnabled = true
    }
    val remoteCacheUrl = providers.gradleProperty("buildCacheUrl").orNull
        ?: providers.environmentVariable("GRADLE_BUILD_CACHE_URL").orNull
    if (remoteCacheUrl != null) {
        remote<HttpBuildCache> {
            url = uri(remoteCacheUrl)
            // Only trusted builds may push to the shared cache.
            isPush = providers.environmentVariable("GRADLE_BUILD_CACHE_PUSH").orNull == "true"
            // Cache credentials travel over plain HTTP; use only a dedicated low-privilege identity.
            isAllowInsecureProtocol =
                providers.environmentVariable("GRADLE_BUILD_CACHE_ALLOW_INSECURE").orNull == "true"
            val user = providers.gradleProperty("buildCacheUsername").orNull
                ?: providers.environmentVariable("GRADLE_BUILD_CACHE_USERNAME").orNull
            val pass = providers.gradleProperty("buildCachePassword").orNull
                ?: providers.environmentVariable("GRADLE_BUILD_CACHE_PASSWORD").orNull
            if (user != null && pass != null) {
                credentials {
                    username = user
                    password = pass
                }
            }
        }
    }
}

rootProject.name = "carbonio-ws-collaboration-ce"

include("carbonio-ws-collaboration-openapi")
include("carbonio-ws-collaboration-core")
include("carbonio-ws-collaboration-it")
include("carbonio-ws-collaboration-boot")
