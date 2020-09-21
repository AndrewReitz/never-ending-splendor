import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.ben-manes.versions") version "0.33.0"
}

subprojects {
    repositories {
        google()
        jcenter()
    }

    tasks.withType<KotlinCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        kotlinOptions.jvmTarget = "1.8"
    }

    tasks.withType<Test> {
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}
