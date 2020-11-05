import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    kotlin("jvm")
    kotlin("kapt")
    idea
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

dependencies {
    implementation(kotlin("stdlib"))

    // api to expose networkingModule.kt to consumers
    api("org.kodein.di:kodein-di:7.1.0")

    // api to expose Interceptors and HttpUrl to consumers
    api("com.squareup.okhttp3:okhttp:4.9.0")

    api("com.natpryce:result4k:2.0.0")

    implementation("com.jakewharton.byteunits:byteunits:0.9.1")

    implementation("com.squareup.okio:okio:2.9.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    implementation("com.squareup.moshi:moshi:1.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.11.0")
    implementation("com.squareup.moshi:moshi-adapters:1.11.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.11.0")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("com.google.truth:truth:1.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.0")
    testImplementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }
}

// setup integration tests w/ intellij/android stuido
// doing it from groovy file because the kotlin dsl is not well equipped to work with this.
apply(from = "idea.gradle")

tasks.register<Test>("integrationTest") {
    val integrationTest by sourceSets
    description = "Runs the integration tests."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    mustRunAfter(tasks.named("test").get())
    outputs.upToDateWhen { false }

    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
