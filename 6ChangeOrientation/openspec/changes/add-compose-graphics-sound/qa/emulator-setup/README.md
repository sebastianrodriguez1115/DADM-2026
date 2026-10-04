# Android emulator setup (local macOS QA)

Local environment prepared for native Android instrumentation; no product, test, Gradle, or OpenSpec task files were changed.

## Host and components

- macOS 27.0.1 on `arm64` (Apple Silicon); Android Emulator reports Hypervisor.Framework acceleration available. No ARM translation required.
- Android SDK: `~/Library/Android/sdk`; Java used for SDK tools and emulator: `/Applications/Android Studio.app/Contents/jbr/Contents/Home` (OpenJDK 25.0.3), scoped to the commands below only. No global Java/PATH configuration changed.
- Already present: Android Emulator 37.2.12 and Platform Tools 37.0.1. Installed only `system-images;android-35;google_apis;arm64-v8a` through the SDK manager. SDK image occupies approximately 3.8 GiB; AVD data approximately 934 MiB. At setup, the SDK volume had 367 GiB free (460 GiB total).
- Existing official `android-sdk-license` acceptance was present; SDK installation completed without a new license prompt. No accounts, paid software, or extra credentials used.
- Created `DADM_GraphicsSound_API35` from the Pixel 6 profile. API 35 / Android 15, Google APIs (no Play Store), `arm64-v8a`, 3072 MiB RAM, 2 virtual CPU cores, audio output enabled, audio input disabled. No existing AVD was overwritten.

## Result

The owned AVD is running and boot-complete on **`emulator-5556`**. Checked its AVD name (`DADM_GraphicsSound_API35`), `sys.boot_completed=1`, API level `35`, release `15`, ABI `arm64-v8a`, and Android framework package path. Its process command line confirms `-avd DADM_GraphicsSound_API35 -port 5556`; the setup log reports boot completed. Hypervisor.Framework acceleration check passed.

### Serial identity incident and mandatory guard

At initial setup this AVD was verified on `emulator-5554`. On the subsequent infrastructure handoff, that serial instead identified an unrelated API 36 AVD. The reason is unknown; serial reuse/port reassignment is the observed fact, not a confirmed cause. The setup agent did not operate on that emulator. A later Worker Gradle connected-device task did reach it because AGP ignored the attempted device filter; the project-root report [`qa/step-5.3/report.md`](../step-5.3/report.md) records that incident and excludes those results as evidence. The owned AVD process had exited; `lsof` showed ports 5556 and 5558 free, so this exact AVD was restarted on port 5556. It now reports its own AVD name, API 35, and `arm64-v8a`. Never trust a remembered serial or act on it before checking identity.

**Before any install, instrumentation/test, or stop action, verify all four identity properties for the chosen emulator serial:**

```sh
SERIAL=emulator-5556
test "$(adb -s "$SERIAL" emu avd name 2>/dev/null | tr -d '\r' | sed -n '1p')" = DADM_GraphicsSound_API35 || { echo 'AVD name mismatch; abort'; exit 1; }
test "$(adb -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')" = 35 || { echo 'API mismatch; abort'; exit 1; }
test "$(adb -s "$SERIAL" shell getprop ro.product.cpu.abi | tr -d '\r')" = arm64-v8a || { echo 'ABI mismatch; abort'; exit 1; }
test "$(adb -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')" = 1 || { echo 'boot incomplete; abort'; exit 1; }
```

If any check fails or the serial is absent, **abort**; enumerate emulator serials only and re-check their name/API/ABI/boot state. Do not query, disclose, disconnect, stop, or alter physical devices. Repeat these checks immediately before every device-targeted operation, especially stop; `ANDROID_SERIAL` is only a selector and not proof of identity.

## Reproduce/start

```sh
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
export PATH="$JAVA_HOME/bin:$HOME/Library/Android/sdk/platform-tools:$PATH"
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"

# Confirm port 5556 is unused before starting (lsof -nP -iTCP:5556 -sTCP:LISTEN).
# Start this AVD only. Cold boot/no snapshot restore; retain this AVD's data.
"$ANDROID_SDK_ROOT/emulator/emulator" -avd DADM_GraphicsSound_API35 \
  -port 5556 -no-snapshot-load -no-snapshot-save -no-boot-anim \
  > "$HOME/.android/DADM_GraphicsSound_API35-emulator.log" 2>&1 &
adb -s emulator-5556 wait-for-device
adb -s emulator-5556 shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 2; done'
```

The emulator was launched as a background process; its setup-time PID is recorded at `~/.android/DADM_GraphicsSound_API35-emulator.pid`. Prefer the explicit serial for all device operations:

```sh
export ANDROID_SERIAL=emulator-5556   # selector only; not an identity check
adb -s emulator-5556 emu avd name     # expected: DADM_GraphicsSound_API35
adb -s emulator-5556 shell getprop ro.build.version.sdk       # expected: 35
adb -s emulator-5556 shell getprop ro.product.cpu.abi          # expected: arm64-v8a
adb -s emulator-5556 shell getprop sys.boot_completed          # expected: 1
```

**Never run any Gradle `connected*` task in this multi-emulator environment.** `ANDROID_SERIAL` does not guarantee filtering of AGP connected-device tasks; `-Pandroid.injected.device.serial` was confirmed ineffective with AGP 9.4.1 and must not be relied on. Do not disconnect or stop other devices to force selection.

Safe automation workflow: run JVM tests and build APKs with Gradle tasks that do not include `connected` (for example `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest`). Then, immediately before **each** install, force-stop, instrumentation invocation, or other device-targeted operation, run the exact AVD-name/API/ABI/boot guard above; act only through `adb -s emulator-5556 ...`. The repository's read-only-reference runner [`qa/final-automation/scripts/run.py`](../final-automation/scripts/run.py) demonstrates this pattern and asserts all four values before each ADB operation. Do not improvise a Gradle connected task or treat an environment/property filter as a substitute for the guard.

Reported final automation evidence records 131 JVM tests and 82 native tests passing through the guarded, serial-targeted APK-install/AndroidJUnitRunner workflow; it is not a `connected*` Gradle run. This does **not** mark task 6.2's literal `./gradlew connectedDebugAndroidTest` as passed absent user approval of an equivalent workflow. Manual listening/audio/TalkBack QA 6.3 remains blocked/pending; see the respective QA reports. This document change neither runs tests nor changes task checkboxes.

## Stop safely

First run the four identity guard checks above immediately against the chosen serial. Only if all pass, this exits that verified owned emulator, preserving its AVD data and leaving the shared ADB server/any physical device alone:

```sh
adb -s emulator-5556 emu kill
```

Avoid `adb kill-server`, `adb disconnect`, `-wipe-data`, and removing/replacing SDK or AVD files. Emulator logs are at `~/.android/DADM_GraphicsSound_API35-emulator.log`.
