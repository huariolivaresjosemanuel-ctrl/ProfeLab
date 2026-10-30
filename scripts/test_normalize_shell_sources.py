from pathlib import Path
import os,tempfile,unittest
from normalize_shell_sources import canonical_shell_bytes,normalize_repo,shell_paths

ROOT=Path(__file__).resolve().parents[1]
class ShellEncodingTest(unittest.TestCase):
    def test_repository_shell_bytes_are_lf_without_bom(self):
        for path in shell_paths(ROOT):
            with self.subTest(path=path.name):
                data=path.read_bytes();self.assertNotIn(b'\r',data);self.assertFalse(data.startswith(b'\xef\xbb\xbf'))
    def test_crlf_and_bom_become_canonical_lf(self):
        self.assertEqual(b'#!/bin/bash\nset -uo pipefail\n',canonical_shell_bytes(b'\xef\xbb\xbf#!/bin/bash\r\nset -uo pipefail\r\n'))
    def test_lf_is_unchanged_and_idempotent(self):
        data=b'#!/bin/bash\nexit 7\n';self.assertEqual(data,canonical_shell_bytes(data));self.assertEqual(data,canonical_shell_bytes(canonical_shell_bytes(data)))
    def test_bare_cr_is_rejected(self):
        with self.assertRaises(ValueError):canonical_shell_bytes(b'echo before\rafter\n')
    def test_check_is_read_only_and_normalize_preserves_other_files(self):
        with tempfile.TemporaryDirectory(dir=os.environ.get('PROFELAB_TEST_TEMP')) as temp:
            root=Path(temp);(root/'scripts').mkdir();script=root/'scripts/run.sh';script.write_bytes(b'exit 0\r\n')
            other=root/'picture.bin';other.write_bytes(b'\x00\r\n\xff');wrapper=root/'gradlew';wrapper.write_bytes(b'#!/bin/sh\n')
            self.assertEqual(['scripts/run.sh'],normalize_repo(root,check=True));self.assertEqual(b'exit 0\r\n',script.read_bytes())
            self.assertEqual(['scripts/run.sh'],normalize_repo(root));self.assertEqual(b'exit 0\n',script.read_bytes());self.assertEqual(b'\x00\r\n\xff',other.read_bytes());self.assertEqual([],normalize_repo(root))

    def test_git_policy_and_ci_bootstrap_are_declared(self):
        policy=(ROOT/'.gitattributes').read_text(encoding='utf-8')
        self.assertIn('*.sh text eol=lf',policy);self.assertIn('gradlew text eol=lf',policy)
        workflow=(ROOT/'.github/workflows/android.yml').read_text(encoding='utf-8')
        self.assertEqual(3,workflow.count('python3 scripts/normalize_shell_sources.py\n'))
        self.assertIn('test_normalize_shell_sources.py',workflow)

if __name__=='__main__':unittest.main()
