#!/bin/bash
# Rony VPN — build the debug APK.
# Needs: JDK 17, Android SDK (platform 36, build-tools 37.0.0,
# NDK 30.0.14904198, CMake 3.22.1). On a normal machine or in Android
# Studio just run the Gradle wrapper; it downloads Gradle 9.7.1 itself.
set -e
cd "$(dirname "$0")/source"
export JAVA_HOME="${JAVA_HOME:-/home/hatch/jdk17}"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug
echo "APK: $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
