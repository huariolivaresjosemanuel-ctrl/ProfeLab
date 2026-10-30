"""Exercise shell status propagation with stub executables; never run adb, Gradle or a real emulator."""
from pathlib import Path
import os,shutil,subprocess,tempfile,unittest

class AndroidOrchestrationTest(unittest.TestCase):
    def scenario(self,upgrade,gradle):
        bash=shutil.which('bash') or ('C:/Program Files/Git/bin/bash.exe' if Path('C:/Program Files/Git/bin/bash.exe').is_file() else None)
        self.assertIsNotNone(bash,'Bash required for this shell-only test')
        with tempfile.TemporaryDirectory(prefix='profelab-shell-',dir=os.environ.get('PROFELAB_TEST_TEMP')) as temp:
            root=Path(temp);(root/'scripts').mkdir();(root/'bin').mkdir()
            shutil.copyfile(Path(__file__).with_name('run_android_checks.sh'),root/'run.sh')
            (root/'scripts/check_android_upgrade.sh').write_text(f'#!/usr/bin/env bash\nexit {upgrade}\n',encoding='utf-8')
            (root/'gradlew').write_text(f'#!/usr/bin/env bash\nprintf called > gradle-called\nexit {gradle}\n',encoding='utf-8')
            (root/'gradlew').chmod(0o755)
            (root/'bin/adb').write_text('#!/usr/bin/env bash\nexit 1\n',encoding='utf-8');(root/'bin/adb').chmod(0o755)
            posix=root.as_posix()
            if os.name=='nt':posix='/'+posix[0].lower()+posix[2:]
            env=dict(os.environ,PATH=posix+'/bin:/usr/bin:/bin')
            result=subprocess.run([bash,'--noprofile','--norc','run.sh'],cwd=root,env=env,capture_output=True,text=True)
            return result.returncode,(root/'gradle-called').exists(),result.stdout+result.stderr
    def test_upgrade_failure_keeps_code_and_skips_gradle(self):
        code,called,log=self.scenario(7,0);self.assertEqual(7,code,log);self.assertFalse(called)
    def test_gradle_failure_keeps_code(self):
        code,called,log=self.scenario(0,9);self.assertEqual(9,code,log);self.assertTrue(called)
    def test_success_is_not_changed_by_missing_optional_screenshot_pull(self):
        code,called,log=self.scenario(0,0);self.assertEqual(0,code,log);self.assertTrue(called);self.assertIn('No screenshots',log)

if __name__=='__main__':unittest.main()
