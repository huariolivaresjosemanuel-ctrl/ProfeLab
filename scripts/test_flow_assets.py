from pathlib import Path
import tempfile,unittest,os
from check_flow_assets import inspect

ROOT=Path(__file__).resolve().parents[1]/'feature/profile/src/main/assets/welcome'
class FlowAssetsTest(unittest.TestCase):
    def test_entry_container_and_all_samples(self):
        data=inspect(ROOT/'flow_entry_v1.mp4');self.assertEqual(87,data['track']['frames']);self.assertEqual(3.625,data['track']['duration_seconds']);self.assertEqual(0,data['audio_tracks'])
    def test_greeting_container_and_all_samples(self):
        data=inspect(ROOT/'flow_greeting_v1.mp4');self.assertEqual(111,data['track']['frames']);self.assertEqual(4.625,data['track']['duration_seconds']);self.assertEqual(0,data['audio_tracks'])
    def test_truncated_video_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=os.environ.get('PROFELAB_TEST_TEMP')) as temp:
            p=Path(temp)/'truncated.mp4';p.write_bytes((ROOT/'flow_entry_v1.mp4').read_bytes()[:-17])
            with self.assertRaises(AssertionError):inspect(p)
    def test_unexpected_audio_track_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=os.environ.get('PROFELAB_TEST_TEMP')) as temp:
            p=Path(temp)/'audio.mp4';p.write_bytes((ROOT/'flow_entry_v1.mp4').read_bytes().replace(b'vide',b'soun',1))
            with self.assertRaises(AssertionError):inspect(p)
if __name__=='__main__':unittest.main()
