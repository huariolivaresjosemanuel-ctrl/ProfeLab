"""Exercise shell status propagation with stub executables; never run adb, Gradle or a real emulator."""
from pathlib import Path
import os,shutil,subprocess,tempfile,unittest
from normalize_shell_sources import canonical_shell_bytes

class AndroidOrchestrationTest(unittest.TestCase):
    def scenario(self,upgrade,gradle,source_bytes=None):
        bash=shutil.which('bash') or ('C:/Program Files/Git/bin/bash.exe' if Path('C:/Program Files/Git/bin/bash.exe').is_file() else None)
        self.assertIsNotNone(bash,'Bash required for this shell-only test')
        with tempfile.TemporaryDirectory(prefix='profelab-shell-',dir=os.environ.get('PROFELAB_TEST_TEMP')) as temp:
            root=Path(temp);(root/'scripts').mkdir();(root/'bin').mkdir()
            source=Path(__file__).with_name('run_android_checks.sh').read_bytes() if source_bytes is None else source_bytes
            (root/'run.sh').write_bytes(canonical_shell_bytes(source))
            (root/'scripts/check_android_upgrade.sh').write_bytes(f'#!/usr/bin/env bash\nexit {upgrade}\n'.encode('utf-8'))
            (root/'gradlew').write_bytes(f'#!/usr/bin/env bash\nprintf called > gradle-called\nexit {gradle}\n'.encode('utf-8'))
            (root/'gradlew').chmod(0o755)
            (root/'bin/adb').write_bytes(b'#!/usr/bin/env bash\nexit 1\n');(root/'bin/adb').chmod(0o755)
            for script in [root/'run.sh',root/'scripts/check_android_upgrade.sh',root/'gradlew',root/'bin/adb']:
                self.assertNotIn(b'\r',script.read_bytes())
            posix=root.as_posix()
            if os.name=='nt':posix='/'+posix[0].lower()+posix[2:]
            env=dict(os.environ,PATH=posix+'/bin:/usr/bin:/bin',GITHUB_ACTIONS='true')
            for name in ['BASH_ENV','ENV','SHELLOPTS','BASHOPTS']:env.pop(name,None)
            result=subprocess.run([bash,'--noprofile','--norc','run.sh'],cwd=root,env=env,capture_output=True,text=True)
            return result.returncode,(root/'gradle-called').exists(),result.stdout+result.stderr
    def test_upgrade_failure_keeps_code_and_skips_gradle(self):
        code,called,log=self.scenario(7,0);self.assertEqual(7,code,log);self.assertFalse(called)
    def test_gradle_failure_keeps_code(self):
        code,called,log=self.scenario(0,9);self.assertEqual(9,code,log);self.assertTrue(called)
    def test_success_is_not_changed_by_missing_optional_screenshot_pull(self):
        code,called,log=self.scenario(0,0);self.assertEqual(0,code,log);self.assertTrue(called);self.assertIn('No screenshots',log)

    def test_crlf_checkout_is_normalized_for_all_exit_paths(self):
        source=canonical_shell_bytes(Path(__file__).with_name('run_android_checks.sh').read_bytes()).replace(b'\n',b'\r\n')
        for upgrade,gradle,expected,called_expected in [(7,0,7,False),(0,9,9,True),(0,0,0,True)]:
            with self.subTest(upgrade=upgrade,gradle=gradle):
                code,called,log=self.scenario(upgrade,gradle,source)
                self.assertEqual(expected,code,log);self.assertEqual(called_expected,called)

if __name__=='__main__':unittest.main()
