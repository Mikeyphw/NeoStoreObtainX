plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.mikeyphw.neostoreobtainx"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.mikeyphw.neostoreobtainx"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.8.1-m9-droidify-scope"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Framework supplies this at runtime: NEVER package XposedBridge classes.
    compileOnly("de.robv.android.xposed:api:82")
    testImplementation("junit:junit:4.13.2")
}
