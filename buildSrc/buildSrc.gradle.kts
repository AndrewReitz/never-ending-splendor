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
    implementation("com.android.tools.build:gradle:4.1.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10")
    implementation("com.google.gms:google-services:4.3.4")
    implementation("com.google.firebase:firebase-crashlytics-gradle:2.3.0")
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
