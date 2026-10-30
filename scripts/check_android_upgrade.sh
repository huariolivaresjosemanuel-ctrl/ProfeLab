#!/usr/bin/env bash
set -euo pipefail

# Dedicated disposable Actions emulator only. Never uninstalls an application.
[[ "${GITHUB_ACTIONS:-}" == "true" ]] || { echo 'Run only in the dedicated Actions emulator.'; exit 1; }
baseline_ref=43ff3eead533105006f7da851784cba7ace063a9
package=pe.profelab.app
hardware=$(adb shell getprop ro.hardware | tr -d '\r')
[[ "$hardware" == "ranchu" || "$hardware" == "goldfish" ]] || { echo 'Refusing a non-emulator device.'; exit 1; }
if adb shell pm list packages "$package" | tr -d '\r' | grep -qx "package:$package"; then
  echo 'Refusing to seed an emulator that already contains ProfeLab. No data was removed.'
  exit 1
fi
output=app/build/reports/androidTests/upgrade
mkdir -p "$output"
baseline_dir=$(mktemp -d "${RUNNER_TEMP:?}/profelab-upgrade.XXXXXX")
git cat-file -e "$baseline_ref^{commit}"
git archive "$baseline_ref" | tar -x -C "$baseline_dir"
chmod +x gradlew "$baseline_dir/gradlew"
env -u PROFELAB_VERSION_CODE "$baseline_dir/gradlew" -p "$baseline_dir" --no-daemon :app:assembleDebug
PROFELAB_VERSION_CODE=5 ./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest
baseline_apk="$baseline_dir/app/build/outputs/apk/debug/app-debug.apk"
current_apk=app/build/outputs/apk/debug/app-debug.apk
tools="${ANDROID_HOME:?}/build-tools/35.0.0"
"$tools/apksigner" verify --print-certs "$baseline_apk" > "$output/baseline-signature.txt"
"$tools/apksigner" verify --print-certs "$current_apk" > "$output/current-signature.txt"
signature(){ awk '/certificate SHA-256 digest:/{print $NF}' "$1" | sort; }
baseline_signature=$(signature "$output/baseline-signature.txt")
current_signature=$(signature "$output/current-signature.txt")
[[ "$baseline_signature" =~ ^[a-fA-F0-9]{64}$ && "$current_signature" =~ ^[a-fA-F0-9]{64}$ ]] || { echo 'Missing or ambiguous certificate digest; no install attempted.'; exit 1; }
[[ "$baseline_signature" == "$current_signature" ]] || { echo 'Certificates differ; no install attempted.'; exit 1; }
"$tools/aapt" dump badging "$baseline_apk" > "$output/baseline-package.txt"
"$tools/aapt" dump badging "$current_apk" > "$output/current-package.txt"
grep -q "package: name='$package' versionCode='4'" "$output/baseline-package.txt"
grep -q "package: name='$package' versionCode='5'" "$output/current-package.txt"
adb install "$baseline_apk"
adb install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
phase(){
  local marker="$1" phase="$2"
  adb shell am instrument -w -r \
    -e class pe.profelab.app.UpgradeSurvivalTest#installedDataSurvivesApkReplacement \
    -e profelab_upgrade_phase "$phase" \
    pe.profelab.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$output/$marker.txt"
  grep -q 'OK (1 test)' "$output/$marker.txt"
  if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$output/$marker.txt"; then exit 1; fi
}
phase before before
adb shell am force-stop "$package"
adb install -r "$current_apk"
phase after-first after
adb shell run-as "$package" cat files/qa-upgrade-ledger.json > "$output/after-first-ledger.json"
PROFELAB_VERSION_CODE=6 ./gradlew --no-daemon :app:assembleDebug
"$tools/apksigner" verify --print-certs "$current_apk" > "$output/next-signature.txt"
[[ "$baseline_signature" == "$(signature "$output/next-signature.txt")" ]]
"$tools/aapt" dump badging "$current_apk" > "$output/next-package.txt"
grep -q "package: name='$package' versionCode='6'" "$output/next-package.txt"
adb shell am force-stop "$package"
adb install -r "$current_apk"
phase after-second after
adb shell run-as "$package" cat files/qa-upgrade-ledger.json > "$output/after-second-ledger.json"
echo 'Upgrade phases completed. These are same-job DEBUG fixture certificates, not proof about the user-installed APK.'
