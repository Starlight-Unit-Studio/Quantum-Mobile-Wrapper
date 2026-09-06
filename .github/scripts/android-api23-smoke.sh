#!/usr/bin/env bash
set -euo pipefail

APK_PATH="${1:-apk/app-debug.apk}"
PACKAGE_NAME="de.starlightunit.game.debug"
ACTIVITY_NAME="de.starlightunit.wrapper.MainActivity"

adb install -r "$APK_PATH"
adb logcat -c
adb shell am start -W -n "${PACKAGE_NAME}/${ACTIVITY_NAME}"

sleep 8

if ! adb shell ps | grep -q "$PACKAGE_NAME"; then
  echo "Quantum Mobile Wrapper process did not survive API 23 cold start." >&2
  adb logcat -d -v time || true
  exit 1
fi

if adb logcat -d -v brief \
  | grep -A 8 -B 3 "Process: ${PACKAGE_NAME}" \
  | grep -q "FATAL EXCEPTION"; then
  echo "Fatal Java exception detected during API 23 startup." >&2
  adb logcat -d -v time || true
  exit 1
fi

echo "Android 6 / API 23 cold-start smoke test passed."
