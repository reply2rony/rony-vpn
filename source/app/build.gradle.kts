plugins {
    alias(libs.plugins.android.application)
    // NOTE: no kotlin.android plugin — AGP 9.x has Kotlin support built in;
    // applying org.jetbrains.kotlin.android fails the build (Run #3).
}

android {
    namespace = "com.rony.vpn"
    compileSdk = 36
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.rony.vpn"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // The :openvpn library module has flavors; we use skeleton + ovpn2.
        missingDimensionStrategy("implementation", "skeleton")
        missingDimensionStrategy("ovpnimpl", "ovpn2")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        debug {
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        disable += setOf("MissingTranslation")
    }
}

dependencies {
    implementation(project(":openvpn"))
    implementation(libs.androidx.appcompat)
    implementation(libs.android.view.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
