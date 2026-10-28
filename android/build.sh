#!/usr/bin/env bash
# Build LyricsStudio.apk by hand — no Gradle required.
# Needs: JDK 17 on PATH, Android SDK build-tools 34 and platform android-34.
#
#   ANDROID_SDK=/path/to/sdk ./build.sh
#
# Output: ../apk/LyricsStudio.apk (signed with a generated self-signed key)
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_SDK:-$HOME/Android/Sdk}"
BT="$SDK/build-tools/34.0.0"
AJ="$SDK/platforms/android-34/android.jar"
APP_ID="com.lyricsstudio.app"

[ -x "$BT/aapt2" ] || { echo "build-tools 34 not found at $BT"; exit 1; }
[ -f "$AJ" ] || { echo "android-34 platform not found at $AJ"; exit 1; }

rm -rf build && mkdir -p build/classes build/dex build/gen

echo "==> resources"
"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/base.apk -I "$AJ" \
  --manifest AndroidManifest.xml -R build/res.zip --java build/gen \
  --auto-add-overlay -A assets --min-sdk-version 23 --target-sdk-version 34

echo "==> java"
javac -source 8 -target 8 -nowarn -bootclasspath "$AJ" -d build/classes \
  "build/gen/${APP_ID//.//}/R.java" java/com/lyricsstudio/app/MainActivity.java

echo "==> dex"
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 23 \
  --lib "$AJ" --output build/dex $(find build/classes -name '*.class')

cp build/base.apk build/app-unsigned.apk
(cd build/dex && zip -q -u ../app-unsigned.apk classes.dex)

echo "==> align + sign"
"$BT/zipalign" -f -p 4 build/app-unsigned.apk build/app-aligned.apk
if [ ! -f keystore.jks ]; then
  keytool -genkeypair -keystore keystore.jks -alias lyricsstudio -keyalg RSA \
    -keysize 2048 -validity 10950 -storepass lyricsstudio -keypass lyricsstudio \
    -dname "CN=Lyrics Studio, OU=Mobile, O=Lyrics Studio, L=India, C=IN"
fi
"$BT/apksigner" sign --ks keystore.jks --ks-key-alias lyricsstudio \
  --ks-pass pass:lyricsstudio --key-pass pass:lyricsstudio --min-sdk-version 23 \
  --out build/LyricsStudio.apk build/app-aligned.apk

mkdir -p ../apk
cp build/LyricsStudio.apk ../apk/LyricsStudio.apk
echo "==> done: ../apk/LyricsStudio.apk"
