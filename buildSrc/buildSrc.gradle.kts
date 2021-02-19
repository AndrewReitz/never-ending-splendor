plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.fabric.io/public")
    jcenter()
}

dependencies {
    implementation("com.android.tools.build:gradle:4.1.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.30")
    implementation("com.google.gms:google-services:4.3.5")
    implementation("com.google.firebase:firebase-crashlytics-gradle:2.5.0")

    implementation("com.squareup:kotlinpoet:1.7.2")
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
