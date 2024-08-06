plugins {
    `kotlin-dsl`
    alias(libs.plugins.version.check)
}

dependencies {
    implementation(libs.bundles.buildSrc)

    // added to get around issue with hilt pulling in an older version
    // maybe remove in future?
    implementation(libs.java.poet)
}
