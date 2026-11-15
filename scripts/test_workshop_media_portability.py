"""Authored media contract checks. Python validation, never Android execution."""
from pathlib import Path
import hashlib,json,unittest

ASSETS=Path(__file__).resolve().parents[1]/'core/content/src/main/assets'
def read(name):return json.loads((ASSETS/name).read_text(encoding='utf-8'))
def digest(data):return hashlib.sha256(data).hexdigest()
def definitions():
    index=read('workshop/catalog_index_v2.json')
    return {r['id']:r for key in ['projectFiles','experimentFiles'] for name in index[key] for r in read('workshop/'+name)['projects']}

class WorkshopMediaPortabilityTest(unittest.TestCase):
    def test_all_eighty_fingerprints_match_authored_text_independent_of_slash_escaping(self):
        catalog=definitions();rows=[r for r in read('workshop/media_slots_v1.json')['resources'] if r['role']=='step_photo'];self.assertEqual(80,len(rows))
        mismatches_without_normalization=[]
        for row in rows:
            steps=catalog[row['contentId']]['steps']
            i=next(i for i,s in enumerate(steps) if (s.get('id') or 'step-'+str(i))==row['stepId'])
            step=steps[i];text=json.dumps([step['title'],step.get('instructionNote','')],ensure_ascii=False,separators=(',',':'))
            self.assertEqual(row['stepFingerprint'],digest(text.encode()),(row['contentId'],row['stepId']))
            android_serialized=text.replace('/','\\/')
            if digest(android_serialized.encode())!=row['stepFingerprint']:mismatches_without_normalization.append((row['contentId'],row['stepId']))
            self.assertEqual(row['stepFingerprint'],digest(android_serialized.replace('\\/','/').encode()))
        self.assertIn(('exp-005','step-0'),mismatches_without_normalization)
    def test_every_declared_slot_has_its_actual_approved_binary(self):
        rows=read('workshop/media_slots_v1.json')['resources'];self.assertEqual(229,len(rows));self.assertEqual(199,len({r['assetPath'] for r in rows}))
        for row in rows:
            path=(ASSETS/row['assetPath']).resolve();self.assertTrue(path.is_relative_to(ASSETS.resolve()))
            data=path.read_bytes();self.assertEqual(row['sha256'],digest(data),row['assetPath'])
            self.assertEqual(row.get('byteSize',len(data)),len(data));self.assertEqual(b'RIFF',data[:4]);self.assertEqual(b'WEBP',data[8:12])
    def test_aliases_keep_the_same_bytes_and_retained_copies_are_not_substituted(self):
        index=read('profelab/media_index_v1.json');by_path={r['assetPath']:r for r in read('workshop/media_slots_v1.json')['resources']};retained=0
        for old,new in index['aliases'].items():
            self.assertEqual(by_path[new]['sha256'],digest((ASSETS/new).read_bytes()))
            legacy=ASSETS/'workshop/images'/old
            if legacy.is_file():self.assertEqual(legacy.read_bytes(),(ASSETS/new).read_bytes());retained+=1
        self.assertEqual(180,retained,'Retain the declared historical copies until Android acceptance')

if __name__=='__main__':unittest.main()
