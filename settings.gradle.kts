rootProject.name = "ailearn"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("org.chromium")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("org.chromium")
            }
        }
        mavenCentral()
        val artifactoryUrl = providers.gradleProperty("artifactory_contextUrl").orNull
        if (!artifactoryUrl.isNullOrBlank()) {
            maven {
                url = uri(artifactoryUrl)
                isAllowInsecureProtocol = true
                content { includeGroupByRegex("prox-lib(\\..*)?") }
                credentials {
                    username = providers.gradleProperty("artifactory_user").orNull
                    password = providers.gradleProperty("artifactory_password").orNull
                }
            }
        }
        maven("https://jitpack.io")
        maven("https://android-sdk.is.com")
        maven("https://artifact.bytedance.com/repository/pangle")
        maven("https://artifacts.applovin.com/android")
        maven("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
    }
}

include(":androidApp")
include(":shared")
