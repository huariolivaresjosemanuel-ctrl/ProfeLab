#!/usr/bin/env bash
set -uo pipefail
# The emulator action executes each script line in a separate shell. Keep this orchestration in one file.
status=0
bash scripts/check_android_upgrade.sh || status=$?
if [ "$status" -eq 0 ]; then
  PROFELAB_VERSION_CODE=6 ./gradlew --no-daemon --stacktrace --continue :core:database:connectedDebugAndroidTest :app:connectedDebugAndroidTest || status=$?
fi
mkdir -p app/build/reports/androidTests/visual
adb pull /sdcard/Android/data/pe.profelab.app.test/files/visual/. app/build/reports/androidTests/visual/ || echo 'No screenshots recovered; inspect the test reports.'
exit "$status"
