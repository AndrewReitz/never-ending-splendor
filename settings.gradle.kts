plugins {
    id("com.gradle.enterprise") version "3.1.1"
}

include(":mobile", "networking")

rootProject.name = "Never-Ending-Splendor"

rootProject.children.forEach {
    if(file("${it.projectDir}/${it.name}.gradle.kts").exists()) {
        it.buildFileName = "${it.name}.gradle.kts"
    } else {
        it.buildFileName = "${it.name}.gradle"
    }
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
