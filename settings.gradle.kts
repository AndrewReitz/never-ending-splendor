enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.enterprise") version "3.8.1"
}

include(":mobile", ":networking")

rootProject.name = "never-ending-splendor"

rootProject.children.forEach {
    it.buildFileName = "${it.name}.gradle.kts"
}

gradleEnterprise {
    buildScan {
        publishAlwaysIf(System.getProperty("NEVER_ENDING_SPLENDOR_ACCEPT_BUILD_SCAN_AGREEMENT") != null)
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = System.getProperty("NEVER_ENDING_SPLENDOR_ACCEPT_BUILD_SCAN_AGREEMENT", "no")
        }
    }
}
