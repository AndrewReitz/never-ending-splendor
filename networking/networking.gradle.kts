plugins {
    idea
    `java-library`

    kotlin("jvm")

    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)

    id("api-key-provider")
    id("kotlin-config-writer")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.coroutines)

    api(libs.okhttp)
    api(libs.bundles.arrow)

    implementation(libs.okio)
    api(libs.bundles.retrofit)

    implementation(libs.kotlinx.serialization)

    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("reflect"))
    testImplementation(libs.bundles.network.test.libs)
    kspTest(libs.dagger.compiler)
}

kotlinConfigWriter {
    packageName = "nes.networking"

    val phishNetApiKey: String by project
    put("PHISH_NET_API_KEY", phishNetApiKey)

    val phishinApiKey: String by project
    put("PHISH_IN_API_KEY", phishinApiKey)
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
val integrationTestRuntimeOnly: Configuration by configurations.getting
configurations["integrationTestImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

val integrationTest = tasks.register<Test>("integrationTest") {
    val integrationTest by sourceSets
    description = "Runs the integration tests."
    group = "verification"

    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter("test")

    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.check { dependsOn(integrationTest) }

idea {
    module {
        testSources.from(sourceSets["integrationTest"].allSource.srcDirs)
    }
}
