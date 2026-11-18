/*
 * ics-openvpn core vendored as a library module.
 * Source: https://github.com/schwabe/ics-openvpn (GPL-2.0, see doc/LICENSE.txt
 * in the upstream copy at ~/workspace/rony-vpn/upstream/ics-openvpn).
 * Only the `skeleton` (no UI) + `ovpn2` (OpenVPN 2.x core, no OpenVPN 3 /
 * no SWIG) flavors are built. The upstream `main` module is an application;
 * this file converts it to a library with the minimum needed to compile the
 * core: aidl + BuildConfig + CMake native build (OpenSSL, lzo, lz4, openvpn).
 * Upstream build script: main/build.gradle.kts in the upstream copy.
 */
plugins {
    alias(libs.plugins.android.library)
    // NOTE: no kotlin.android plugin — AGP 9.x has Kotlin support built in;
    // applying org.jetbrains.kotlin.android fails the build (Run #3).
}

android {
    namespace = "de.blinkt.openvpn"
    compileSdk = 36
    buildToolsVersion = "37.0.0"
    ndkVersion = "30.0.14904198"

    defaultConfig {
        minSdk = 23
        externalNativeBuild {
            cmake {
            }
        }
        ndk {
            // Phones are ARM; skip x86/x86_64 to halve the native build time.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = File("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        getByName("main") {
            // The native build copies the pie_openvpn binaries here (see
            // src/main/cpp/CMakeLists.txt: OVPN_ASSET_DIR).
            assets.directories.add("build/ovpnassets")
        }
    }

    flavorDimensions += listOf("implementation", "ovpnimpl")

    productFlavors {
        create("skeleton") {
            dimension = "implementation"
        }
        create("ovpn2") {
            dimension = "ovpnimpl"
            buildConfigField("boolean", "openvpn3", "false")
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
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin)
}
