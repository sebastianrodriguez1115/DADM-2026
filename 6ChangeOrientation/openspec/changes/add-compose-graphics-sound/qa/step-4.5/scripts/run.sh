#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../../../../.." && pwd)"
QA="$ROOT/openspec/changes/add-compose-graphics-sound/qa/step-4.5"
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
export ANDROID_SDK_ROOT='/Users/jsrc1115/Library/Android/sdk'
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
SERIAL=emulator-5556
mkdir -p "$QA/evidence/jvm"
cd "$ROOT"
guard() {
  local name sdk abi boot
  name=$(adb -s "$SERIAL" emu avd name | tr -d '\r' | sed -n '1p')
  sdk=$(adb -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')
  abi=$(adb -s "$SERIAL" shell getprop ro.product.cpu.abi | tr -d '\r')
  boot=$(adb -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')
  printf '%s action=%s serial=%s avd=%s sdk=%s abi=%s boot=%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$1" "$SERIAL" "$name" "$sdk" "$abi" "$boot" | tee -a "$QA/evidence/device-identity.log"
  test "$name" = DADM_GraphicsSound_API35 && test "$sdk" = 35 && test "$abi" = arm64-v8a && test "$boot" = 1 || { echo 'Identity mismatch: ABORT'; exit 1; }
}
date -u '+%Y-%m-%dT%H:%M:%SZ' >> "$QA/evidence/start.txt"
guard preflight
if test "${1:-}" != --native-only; then
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --offline --no-configuration-cache --rerun-tasks > "$QA/evidence/gradle.log" 2>&1
echo 'gradle_exit=0' > "$QA/evidence/exits.log"
cp app/build/test-results/testDebugUnitTest/TEST-*.xml "$QA/evidence/jvm/"
guard install-app
adb -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk > "$QA/evidence/install-app.log" 2>&1
echo 'install_app_exit=0' >> "$QA/evidence/exits.log"
guard install-test
adb -s "$SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk > "$QA/evidence/install-test.log" 2>&1
echo 'install_test_exit=0' >> "$QA/evidence/exits.log"
fi
for class in AndroidMoveSoundPlayerLoadingTest MainActivityLifecycleTest MainActivityTest MainActivityPendingTurnOptionsTest TicTacToeScreenTest; do
  guard "instrument-$class"
  set +e
  adb -s "$SERIAL" shell am instrument -w -r -e class "com.edu.unal.tictactoe.$class" com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner > "$QA/evidence/$class.log" 2>&1
  rc=$?
  set -e
  echo "${class}_adb_exit=$rc" >> "$QA/evidence/exits.log"
  test "$rc" = 0 || exit "$rc"
  python3 "$QA/scripts/summarize.py" --native "$QA/evidence/$class.log"
done
python3 "$QA/scripts/summarize.py"
date -u '+%Y-%m-%dT%H:%M:%SZ' > "$QA/evidence/end.txt"
