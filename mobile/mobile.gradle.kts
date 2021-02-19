plugins {
    id("com.android.application")
    id("kotlin-android")

    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")

    id("api-key-provider")
    id("signing-config")
    id("build-number")

    id("com.github.triplet.play") version "3.2.0"
}

play {
    serviceAccountCredentials.set(
        rootProject.file(properties["never.ending.splendor.publish-key"] ?: "keys/publish-key.json")
    )
    track.set("internal")
    defaultToAppBundles.set(true)
}

android {
    compileSdkVersion(30)
    buildToolsVersion("29.0.3")

    signingConfigs {
        val keystoreLocation: String by project
        val keystorePassword: String by project
        val storeKeyAlias: String by project
        val aliasKeyPassword: String by project

        val debug by getting {
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
        minSdkVersion(23)
        targetSdkVersion(30)
        versionCode = buildNumber.toInt()
        versionName = "About to Run"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
        }
        val release by getting {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    testOptions.unitTests.isReturnDefaultValues = true
    buildFeatures.viewBinding = true
}

dependencies {
    implementation(project(":networking"))

    implementation(kotlin("stdlib"))
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2")

    implementation("com.google.android.gms:play-services-cast:19.0.0")
    implementation("com.google.android.libraries.cast.companionlibrary:ccl:2.9.1")
    implementation("com.google.android.material:material:1.3.0")

    implementation(platform("com.google.firebase:firebase-bom:26.5.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.mediarouter:mediarouter:1.2.2")
    implementation("androidx.media2:media2-session:1.1.2")
    implementation("androidx.media2:media2-widget:1.1.2")
    implementation("androidx.media2:media2-player:1.1.2")

    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    implementation("org.kodein.di:kodein-di:7.3.1")
    implementation("org.kodein.di:kodein-di-framework-android-x:7.3.1")

    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")

    implementation("com.jakewharton.timber:timber:4.7.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.1.2")
    testImplementation("org.mockito:mockito-core:3.7.7")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}
