#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../../../../.." && pwd)"
QA="$ROOT/openspec/changes/add-compose-graphics-sound/qa/step-5.3"
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
export ANDROID_HOME='/Users/jsrc1115/Library/Android/sdk'
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
ADB="$ANDROID_HOME/platform-tools/adb"
SERIAL=emulator-5556
mkdir -p "$QA/evidence"
cd "$ROOT"
: > "$QA/evidence/exits.txt"
: > "$QA/evidence/device-guards.log"
guard() {
  local name sdk abi boot
  name="$($ADB -s "$SERIAL" emu avd name | tr -d '\r' | sed -n '1p')"
  sdk="$($ADB -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"
  abi="$($ADB -s "$SERIAL" shell getprop ro.product.cpu.abi | tr -d '\r')"
  boot="$($ADB -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')"
  printf '%s serial=%s name=%s sdk=%s abi=%s boot=%s\n' "$(date -u +%FT%TZ)" "$SERIAL" "$name" "$sdk" "$abi" "$boot" | tee -a "$QA/evidence/device-guards.log"
  [[ "$name" = DADM_GraphicsSound_API35 && "$sdk" = 35 && "$abi" = arm64-v8a && "$boot" = 1 ]]
}
run_logged() {
  local label="$1" rc
  shift
  set +e
  "$@" > "$QA/evidence/$label.log" 2>&1
  rc=$?
  set -e
  printf '%s_exit=%s\n' "$label" "$rc" >> "$QA/evidence/exits.txt"
  if [[ "$rc" != 0 ]]; then cat "$QA/evidence/$label.log"; return "$rc"; fi
}
fingerprint() {
  python3 - "$ROOT" "$QA/evidence/$1.json" <<'PY'
import hashlib, json, pathlib, sys
root = pathlib.Path(sys.argv[1])
paths = list((root/'app/src').rglob('*'))
paths += list((root/'openspec/changes/add-compose-graphics-sound/qa/step-5.2').rglob('*'))
paths += [root/'openspec/changes/add-compose-graphics-sound/tasks.md']
result = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
          for p in sorted(paths) if p.is_file()}
pathlib.Path(sys.argv[2]).write_text(json.dumps(result, indent=2)+'\n')
PY
}
guard
git status --short > "$QA/evidence/git-status-before.txt"
fingerprint protected-files-before
run_logged build ./gradlew --offline --no-configuration-cache :app:assembleDebug :app:assembleDebugAndroidTest
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk > "$QA/evidence/apk-sha256.txt"
guard
run_logged install-app "$ADB" -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
grep -q '^Success' "$QA/evidence/install-app.log"
guard
run_logged install-test "$ADB" -s "$SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
grep -q '^Success' "$QA/evidence/install-test.log"
for suite in GameScreenSoundIntegrationTest GameMenuTest; do
  case "$suite" in GameScreenSoundIntegrationTest) count=1;; GameMenuTest) count=6;; esac
  guard
  run_logged "$suite" "$ADB" -s "$SERIAL" shell am instrument -w -r -e class "com.edu.unal.tictactoe.$suite" com.edu.unal.tictactoe.test/androidx.test.runner.AndroidJUnitRunner
  python3 - "$QA/evidence/$suite.log" "$count" > "$QA/evidence/$suite-validation.json" <<'PY'
import json, pathlib, re, sys
text = pathlib.Path(sys.argv[1]).read_text()
expected = int(sys.argv[2])
codes = re.findall(r'^INSTRUMENTATION_STATUS_CODE: (-?\d+)\s*$', text, re.M)
methods = re.findall(r'^INSTRUMENTATION_STATUS: test=(.+)$', text, re.M)
starts, passed = codes.count('1'), codes.count('0')
ok = bool(re.search(rf'^OK \({expected} tests?\)\s*$', text, re.M))
finished = bool(re.search(r'^INSTRUMENTATION_CODE: -1\s*$', text, re.M))
failed = bool(re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed', text))
valid = starts == passed == expected and ok and finished and not failed and all(c in ('0','1') for c in codes)
print(json.dumps(dict(expected=expected, starts=starts, passed=passed, status_codes=codes,
                      methods=methods, ok_summary=ok, finished=finished, failure_marker=failed, verified_pass=valid), indent=2))
assert valid, 'Instrumentation not proven PASS'
PY
  cat "$QA/evidence/$suite-validation.json"
  printf '%s_verified_pass=%s\n' "$suite" "$count" >> "$QA/evidence/exits.txt"
done
fingerprint protected-files-after
cmp "$QA/evidence/protected-files-before.json" "$QA/evidence/protected-files-after.json"
git status --short > "$QA/evidence/git-status-after.txt"
printf 'protected_files_unchanged=1\n' >> "$QA/evidence/exits.txt"
