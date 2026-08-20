#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ -x ./gradlew ]]; then
  ./gradlew assembleDebug
elif [[ -x /tmp/gradle-8.9/bin/gradle ]]; then
  /tmp/gradle-8.9/bin/gradle wrapper --gradle-version 8.9
  ./gradlew assembleDebug
else
  echo "Gradle wrapper is missing. Run Gradle 8.9 wrapper once, then rerun this script." >&2
  exit 1
fi
cp app/build/outputs/apk/debug/app-debug.apk ../QuickContactsWidget-v1.4.0.apk
echo "APK: $(cd .. && pwd)/QuickContactsWidget-v1.4.0.apk"
