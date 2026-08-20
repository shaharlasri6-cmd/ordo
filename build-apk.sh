#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
VERSION=$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts)
./gradlew assembleDebug
OUTPUT="../Ordo-v${VERSION}.apk"
cp app/build/outputs/apk/debug/app-debug.apk "$OUTPUT"
echo "APK: $(realpath "$OUTPUT")"
