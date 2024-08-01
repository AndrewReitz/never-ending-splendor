import org.jetbrains.kotlin.utils.addToStdlib.cast

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)

    id("build-number")
}

kotlin {
    jvmToolchain(11)
}

android {
    namespace = "nes.app"
    compileSdk = libs.versions.android.sdk.get().toInt()

    signingConfigs {

        getByName("debug") {
            storeFile = rootProject.file("keys/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        val buildNumber: String by project
        applicationId = "nes.app"
        minSdk = 23
        targetSdk = libs.versions.android.sdk.get().toInt()
        versionCode = buildNumber.toInt()
        versionName = "A Wave of Hope"
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

hilt {
    enableAggregatingTask = false
}

dependencies {
    implementation(projects.networking)
    implementation(kotlin("stdlib"))

    implementation(libs.kotlinx.serialization)
    implementation(libs.android.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.navigation)

    implementation(libs.bundles.hilt)
    ksp(libs.hilt.android.compiler)

    implementation(libs.accompanist.systemuicontroller)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.timber)

    implementation(libs.bundles.media3)
    implementation(libs.androidx.mediarouter)

    debugImplementation(libs.bundles.android.debug.libs)
    releaseImplementation(libs.bundles.android.release.libs)

    testImplementation(libs.bundles.android.test.libs)
}
