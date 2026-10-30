#!/usr/bin/env bash
set -uo pipefail
[[ "${GITHUB_ACTIONS:-}" == "true" ]] || { echo "CI-only orchestration; no device command was run."; exit 1; }
# The emulator action executes each script line in a separate shell. Keep this orchestration in one file.
status=0
bash scripts/check_android_upgrade.sh || status=$?
if [ "$status" -eq 0 ]; then
  PROFELAB_VERSION_CODE=6 ./gradlew --no-daemon --stacktrace --continue :core:database:connectedDebugAndroidTest :app:connectedDebugAndroidTest || status=$?
fi
mkdir -p app/build/reports/androidTests/visual
capture_archive=$(mktemp)
if adb exec-out run-as pe.profelab.app tar -C files/qa-captures -cf - visual > "$capture_archive"; then
  tar -xf "$capture_archive" -C app/build/reports/androidTests || echo 'No screenshots recovered; inspect the test reports.'
else
  echo 'No screenshots recovered; inspect the test reports.'
fi
rm -f "$capture_archive"
exit "$status"
