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
    id("com.gradle.develocity") version "3.17.6"
}

include(":mobile", ":networking")

rootProject.name = "never-ending-splendor"

rootProject.children.forEach {
    it.buildFileName = "${it.name}.gradle.kts"
}

develocity {
    buildScan {
        publishing.onlyIf { System.getProperty("NEVER_ENDING_SPLENDOR_ACCEPT_BUILD_SCAN_AGREEMENT") != null }
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set(System.getProperty("NEVER_ENDING_SPLENDOR_ACCEPT_BUILD_SCAN_AGREEMENT", "no"))
    }
}
