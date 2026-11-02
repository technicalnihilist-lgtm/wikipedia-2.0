#!/usr/bin/env bash
# Builds Wikipedia-2.0.apk without Gradle, using the Android SDK build tools.
# Requires: JDK 17, aapt2, d8 (r8.jar), apksig.jar, and an android.jar (API 30+).
# Set these before running:
#   AJAR=/path/to/android.jar  AAPT=/path/to/aapt2  R8=/path/to/r8.jar  APKSIG=/path/to/apksig.jar
set -e
: "${AJAR:?android.jar}"; : "${AAPT:?aapt2}"; : "${R8:?r8.jar}"; : "${APKSIG:?apksig.jar}"

$AAPT compile --dir res -o res.zip
$AAPT link -o base.apk -I "$AJAR" --manifest AndroidManifest.xml -R res.zip \
  --java gen --min-sdk-version 21 --target-sdk-version 28 -A assets

javac -source 8 -target 8 -nowarn -bootclasspath "$AJAR" -classpath "$AJAR" -d classes \
  java/com/wikipedia20/app/MainActivity.java gen/com/wikipedia20/app/R.java

mkdir -p dexout
java -cp "$R8" com.android.tools.r8.D8 --min-api 21 --lib "$AJAR" \
  --output dexout $(find classes -name '*.class')

cp base.apk unsigned.apk
(cd dexout && zip -q -X ../unsigned.apk classes.dex)

# sign (expects w20.p12 with alias w20, password "android")
java -cp "$APKSIG:." Sign unsigned.apk Wikipedia-2.0.apk w20.p12 w20 android
