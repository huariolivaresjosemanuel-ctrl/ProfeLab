import base64, hashlib, os, pathlib, tempfile, unittest
from types import SimpleNamespace
from signing import prepare, cleanup, verify

class SigningTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(dir=os.environ.get('PROFELAB_TEST_TEMP'));self.addCleanup(self.temp.cleanup)
        self.root=pathlib.Path(self.temp.name)
        self.env={'RUNNER_TEMP':str(self.root),'PROFELAB_KEYSTORE_BASE64':base64.b64encode(b'unit-test-not-a-real-key').decode(),
            'PROFELAB_KEYSTORE_PASSWORD':'test-only','PROFELAB_KEY_ALIAS':'test-only','PROFELAB_KEY_PASSWORD':'test-only',
            'PROFELAB_SIGNING_CERT_SHA256':'a'*64,'PROFELAB_VERSION_CODE':'1008',
            'PROFELAB_LAST_DISTRIBUTED_VERSION_CODE':'1','PROFELAB_APKSIGNER':'not-executed','PROFELAB_AAPT':'fake-aapt'}
    def test_missing_configuration_never_writes_key(self):
        for name in ['PROFELAB_KEYSTORE_BASE64','PROFELAB_KEYSTORE_PASSWORD','PROFELAB_KEY_ALIAS','PROFELAB_KEY_PASSWORD','PROFELAB_SIGNING_CERT_SHA256']:
            env=self.env.copy();env.pop(name)
            with self.assertRaises(ValueError):prepare(env)
            self.assertFalse((self.root/'profelab-signing').exists())
    def test_invalid_base64_rejected(self):
        self.env['PROFELAB_KEYSTORE_BASE64']='!invalid!'
        with self.assertRaises(ValueError):prepare(self.env)
    def test_rollback_and_same_version_rejected(self):
        for last in ['1008','1009']:
            self.env['PROFELAB_LAST_DISTRIBUTED_VERSION_CODE']=last
            with self.assertRaises(ValueError):prepare(self.env)
    def test_prepare_cleanup_outside_checkout(self):
        p=prepare(self.env);self.assertEqual(self.root/'profelab-signing/release.keystore',p)
        self.assertTrue(p.is_file());cleanup(self.env);cleanup(self.env)
        self.assertFalse(p.parent.exists())
    def test_existing_directory_not_overwritten(self):
        prepare(self.env)
        with self.assertRaises(ValueError):prepare(self.env)
    def test_invalid_certificate_pin_rejected(self):
        self.env['PROFELAB_SIGNING_CERT_SHA256']='invalid'
        with self.assertRaises(ValueError):prepare(self.env)
    def verification(self, digest='a'*64, code=0, v2='true', package='pe.profelab.app'):
        apk=self.root/'input.apk';apk.write_bytes(b'unit-test-not-an-apk')
        output=self.root/'public'
        result=SimpleNamespace(returncode=code,stdout=f'Signer #1 certificate SHA-256 digest: {digest}\nVerified using v2 scheme (APK Signature Scheme v2): {v2}\n')
        def run(args,**kwargs):
            if args[0]=='fake-aapt': return SimpleNamespace(returncode=0,stdout=f"package: name='{package}' versionCode='1008' versionName='0.1.1-h1.1'\n")
            return result
        verify(self.env,apk,output,run=run)
        return output
    def test_wrong_certificate_never_published(self):
        with self.assertRaises(ValueError):self.verification(digest='b'*64)
        self.assertFalse((self.root/'public').exists())
    def test_failed_apksigner_never_published(self):
        with self.assertRaises(ValueError):self.verification(code=1)
        self.assertFalse((self.root/'public').exists())
    def test_v2_required(self):
        with self.assertRaises(ValueError):self.verification(v2='false')
    def test_wrong_application_never_published(self):
        with self.assertRaises(ValueError):self.verification(package='wrong.app')
        self.assertFalse((self.root/'public').exists())
    def test_verified_output_contains_only_public_files(self):
        output=self.verification()
        self.assertEqual({'ProfeLab-H1.1-release.apk','SHA256SUMS.txt','firma-publica.json'},{p.name for p in output.iterdir()})
        self.assertIn(hashlib.sha256(b'unit-test-not-an-apk').hexdigest(),(output/'SHA256SUMS.txt').read_text())

if __name__=='__main__':unittest.main()
