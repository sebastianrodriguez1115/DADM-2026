#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../../../../.." && pwd)"
QA="$ROOT/openspec/changes/add-compose-graphics-sound/qa/step-5.2"
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
export ANDROID_HOME='/Users/jsrc1115/Library/Android/sdk'
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
SERIAL=emulator-5556
ADB="$ANDROID_HOME/platform-tools/adb"
mkdir -p "$QA/evidence"
cd "$ROOT"
guard() {
  local name sdk abi boot
  name="$($ADB -s "$SERIAL" emu avd name | tr -d '\r' | sed -n '1p')"
  sdk="$($ADB -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"
  abi="$($ADB -s "$SERIAL" shell getprop ro.product.cpu.abi | tr -d '\r')"
  boot="$($ADB -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')"
  printf '%s serial=%s name=%s sdk=%s abi=%s boot=%s\n' "$(date -u +%FT%TZ)" "$SERIAL" "$name" "$sdk" "$abi" "$boot" | tee -a "$QA/evidence/device-guards.log"
  [[ "$name" = DADM_GraphicsSound_API35 && "$sdk" = 35 && "$abi" = arm64-v8a && "$boot" = 1 ]]
}
guard
git status --short > "$QA/evidence/git-status-before.txt"
./gradlew --offline --no-configuration-cache :app:assembleDebug :app:assembleDebugAndroidTest > "$QA/evidence/build.log" 2>&1
printf 'build_exit=0\n' > "$QA/evidence/exits.txt"
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk > "$QA/evidence/apk-sha256.txt"
guard
$ADB -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk > "$QA/evidence/install-app.log" 2>&1
printf 'install_app_exit=0\n' >> "$QA/evidence/exits.txt"
guard
$ADB -s "$SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk > "$QA/evidence/install-test.log" 2>&1
printf 'install_test_exit=0\n' >> "$QA/evidence/exits.txt"
for suite in GameMenuTest GameScreenTest MainActivityPendingTurnOptionsTest; do
  case "$suite" in GameMenuTest) count=6;; GameScreenTest) count=7;; *) count=3;; esac
  guard
  set +e
  $ADB -s "$SERIAL" shell am instrument -w -r -e class "com.edu.unal.tictactoe.$suite" com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner > "$QA/evidence/$suite.log" 2>&1
  status=$?
  set -e
  printf '%s_adb_exit=%s\n' "$suite" "$status" >> "$QA/evidence/exits.txt"
  python3 - "$QA/evidence/$suite.log" "$count" <<'PY'
import pathlib, re, sys
text = pathlib.Path(sys.argv[1]).read_text()
expected = int(sys.argv[2])
starts = len(re.findall(r'^INSTRUMENTATION_STATUS_CODE: 1\s*$', text, re.M))
passed = len(re.findall(r'^INSTRUMENTATION_STATUS_CODE: 0\s*$', text, re.M))
codes = re.findall(r'^INSTRUMENTATION_STATUS_CODE: (-?\d+)\s*$', text, re.M)
ok = bool(re.search(rf'^OK \({expected} tests\)\s*$', text, re.M))
print(f'{sys.argv[1]}: starts={starts}, passed={passed}, expected={expected}, codes={codes}, OK={ok}')
assert starts == passed == expected and ok and all(c in ('0', '1') for c in codes), 'Instrumentation not proven PASS'
PY
  [[ "$status" = 0 ]]
  printf '%s_verified_pass=%s\n' "$suite" "$count" >> "$QA/evidence/exits.txt"
done
git status --short > "$QA/evidence/git-status-after.txt"
