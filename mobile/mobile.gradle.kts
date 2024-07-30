import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.serialization)

    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)

    id("api-key-provider")
    id("signing-config")
    id("build-number")

    alias(libs.plugins.play.publisher)
}

kotlin {
    jvmToolchain(11)
}

play {
    serviceAccountCredentials.set(
        rootProject.file(properties["never.ending.splendor.publish-key"] ?: "keys/publish-key.json")
    )
    track.set("internal")
    defaultToAppBundles.set(true)
}

android {
    namespace = "never.ending.splendor"
    compileSdk = libs.versions.android.sdk.get().toInt()

    signingConfigs {
        val keystoreLocation: String by project
        val keystorePassword: String by project
        val storeKeyAlias: String by project
        val aliasKeyPassword: String by project

        getByName("debug") {
            storeFile = rootProject.file("keys/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = rootProject.file(keystoreLocation)
            storePassword = keystorePassword
            keyAlias = storeKeyAlias
            keyPassword = aliasKeyPassword
        }
    }

    defaultConfig {
        val buildNumber: String by project
        applicationId = "never.ending.splendor"
        minSdk = 23
        targetSdk = libs.versions.android.sdk.get().toInt()
        versionCode = buildNumber.toInt()
        versionName = "First Tube"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
            (this as ExtensionAware).configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        val release by getting {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    testOptions.unitTests.isReturnDefaultValues = true
    buildFeatures {
        viewBinding = true
        aidl = false
        buildConfig = false
        compose = true
        prefab = false
        renderScript = false
        resValues = false
        shaders = false
    }
}

dependencies {
    implementation(projects.networking)
    implementation(kotlin("stdlib"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.play.services.cast)
    implementation(libs.play.services.cast.companion)

    implementation(platform(libs.firebase.bom))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.firebase)

    implementation(libs.bundles.androidx)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.navigation)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.kodein)
    implementation(libs.kodein.compose)

    implementation(libs.picasso)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.timber)

    debugImplementation(libs.bundles.android.debug.libs)
    releaseImplementation(libs.bundles.android.release.libs)

    testImplementation(libs.bundles.android.test.libs)
}
