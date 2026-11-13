"""Reorganize APK editorial media only, retaining aliases for old saved references."""
from pathlib import Path
from collections import defaultdict
import argparse,hashlib,json,re

def sha(data):return hashlib.sha256(data).hexdigest()
def write_json(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
def modularize(project):
    project=Path(project).resolve();assets=project/'core/content/src/main/assets';workshop=assets/'workshop'
    registry=json.loads((workshop/'media_slots_v1.json').read_text(encoding='utf-8'));rows=registry['resources']
    assert rows and len(rows)<=4000
    index=json.loads((workshop/'catalog_index_v2.json').read_text(encoding='utf-8'));categories={}
    for key,default in [('projectFiles','projects'),('experimentFiles','experiments')]:
        for filename in index[key]:
            assert re.fullmatch(r'[a-zA-Z0-9_-]+\.json',filename)
            catalog=json.loads((workshop/filename).read_text(encoding='utf-8'))
            for activity in catalog['projects']:
                category=activity.get('categoryId',default)
                categories[activity['id']]=category if category in {'cooking','dance','heritage','ideas'} else default
    groups=defaultdict(list)
    for row in rows:
        assert row['contentId'] in categories
        assert row['role'] in {'catalog_cover','detail_hero','step_photo','material_icon','outcome_reference','teacher_reference'}
        assert re.fullmatch(r'[a-f0-9]{64}',row['sha256'])
        groups[row['sha256']].append(row)
    aliases={};physical=[];binaries={};migration=[];used_paths={}
    role_folder={'catalog_cover':'cover','detail_hero':'cover','step_photo':'steps','outcome_reference':'outcomes','teacher_reference':'teacher'}
    priority={'cover':0,'steps':1,'outcomes':2,'teacher':3}
    for digest,bindings in sorted(groups.items()):
        old=bindings[0].get('legacyAssetFilename') or bindings[0]['assetFilename']
        assert re.fullmatch(r'[a-zA-Z0-9_-]+\.webp',old)
        possible=[workshop/'images'/old]
        if bindings[0].get('assetPath'):
            reference=bindings[0]['assetPath']
            assert reference.startswith('profelab/') and '\\' not in reference and ':' not in reference and '%' not in reference
            assert all(part and part not in {'.','..'} for part in reference.split('/'))
            candidate=assets/reference;assert candidate.resolve().is_relative_to(assets.resolve())
            possible.append(candidate)
        source=next((p for p in possible if p.is_file()),None);assert source is not None,old
        binary=source.read_bytes();assert sha(binary)==digest
        assert all(r['sha256']==digest and (r.get('legacyAssetFilename') or r.get('assetFilename'))==old for r in bindings)
        first=bindings[0]
        assert all((r['mime'],r['width'],r['height'])==(first['mime'],first['width'],first['height']) for r in bindings)
        if any(r['role']=='material_icon' for r in bindings):folder='profelab/shared/materials'
        elif len({r['contentId'] for r in bindings})>1:folder='profelab/shared/media'
        else:
            content=first['contentId'];slug=re.sub(r'[^a-zA-Z0-9_-]','_',content)
            if slug!=content:slug=slug[:80]+'_'+sha(content.encode())[:10]
            assert re.fullmatch(r'[a-zA-Z0-9][a-zA-Z0-9_-]{0,119}',slug)
            role=min((role_folder[r['role']] for r in bindings),key=lambda r:priority[r])
            folder=f'profelab/taller/{categories[content]}/{slug}/{role}'
        path=f'{folder}/h_{digest[:24]}.webp'
        assert path not in used_paths or used_paths[path]==digest,'Filename hash-prefix collision'
        used_paths[path]=digest;aliases[old]=path;binaries[path]=binary
        physical.append(dict(assetPath=path,legacyAssetFilename=old,sha256=digest,byteSize=len(binary),mime=first['mime'],width=first['width'],height=first['height'],
            bindings=[{k:r[k] for k in ['contentId','contentVersion','role','stepId','materialId'] if k in r} for r in bindings]))
        migration.append(dict(legacyAssetFilename=old,oldAssetPath='workshop/images/'+old,assetPath=path,
            oldPath='core/content/src/main/assets/workshop/images/'+old,newPath='core/content/src/main/assets/'+path,sha256=digest,bytes=len(binary)))
        for row in bindings:
            row['legacyAssetFilename']=old;row['assetPath']=path;row.pop('assetFilename',None)
    assert len(aliases)==len(physical)==len(binaries)
    # Stage and verify every destination before removing any redundant file in this local copy.
    for path,binary in binaries.items():
        target=assets/path;assert target.resolve().is_relative_to(assets.resolve())
        target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(binary)
        assert sha(target.read_bytes())==sha(binary)
    write_json(assets/'profelab/media_index_v1.json',dict(schemaVersion=1,assetLayoutVersion=1,scope='TALLER_ONLY',aliases=aliases,resources=physical))
    registry['assetLayoutVersion']=1;registry['mediaIndex']='profelab/media_index_v1.json'
    write_json(workshop/'media_slots_v1.json',registry)
    write_json(project/'docs/taller/T3_0/MEDIA_PATH_MIGRATION.json',dict(schemaVersion=1,scope='APK_EDITORIAL_ASSETS_ONLY',
        note='Private UUID files and offline packs are unchanged. Aliases preserve old assetFilename references; old files may be removed after loaders/index are applied.',
        uniqueFiles=len(physical),bindings=len(rows),files=migration))
    receipt_path=project/'docs/taller/T3_0/BANCO_VISUAL_INTEGRADO.json'
    if receipt_path.is_file():
        receipt=json.loads(receipt_path.read_text(encoding='utf-8'));receipt['assetLayoutVersion']=1
        for row in receipt.get('resources',[]):
            old=row.get('legacyAssetFilename') or row.get('assetFilename')
            if old in aliases:row['legacyAssetFilename']=old;row['assetPath']=aliases[old];row.pop('assetFilename',None)
        write_json(receipt_path,receipt)
    # Keep legacy files until real Android loading/update tests have been accepted.
    for row in migration:
        old=project/row['oldPath']
        if old.is_file():
            assert old.resolve().parent==(workshop/'images').resolve()
            assert sha(old.read_bytes())==row['sha256']==sha((project/row['newPath']).read_bytes())
    return dict(unique_files=len(physical),bindings=len(rows),aliases=len(aliases),bytes=sum(len(b) for b in binaries.values()))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--project',type=Path,default=Path(__file__).resolve().parents[1]);a=p.parse_args()
    print(json.dumps(modularize(a.project),ensure_ascii=False,indent=2))
