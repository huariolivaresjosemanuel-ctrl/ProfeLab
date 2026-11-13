"""Integrate explicitly approved editorial WebP bindings, without touching personal data."""
from pathlib import Path, PurePosixPath
import argparse,collections,hashlib,json,zipfile

def sha(data):return hashlib.sha256(data).hexdigest()
def compact(value):return json.dumps(value,ensure_ascii=False,separators=(',',':'))
def integrate(bank,project,approval_order):
    project=Path(project).resolve();assets=project/'core/content/src/main/assets/workshop'
    with zipfile.ZipFile(bank) as z:
        names=z.namelist();assert len(names)==len(set(names))
        assert all(not n.startswith('/') and '\\' not in n and '..' not in PurePosixPath(n).parts for n in names)
        original=z.read('07_MANIFIESTO_MEDIOS.json');proposal=z.read('13_PROPUESTA_VINCULOS_EDITORIALES.json')
        manifest=json.loads(original);selection=json.loads(proposal)['proposedBindings']
        assert manifest['scope']=='TALLER_ONLY' and len(selection)==229
        definitions={}
        for filename in ['projects.json','experiments.json']:
            root=json.loads((assets/filename).read_text(encoding='utf-8'))
            for row in root['projects']:
                assert row['id'] not in definitions
                definitions[row['id']]={**row,'contentVersion':row.get('contentVersion',root['version'])}
        assert len(definitions)==20
        materials={x['id'] for x in json.loads((assets/'materials_v2.json').read_text(encoding='utf-8'))['materials']}
        assert len(materials)==45
        physical={r['relativePath']:r for r in manifest['files']}
        associations={(r['contentId'],r['role'],r.get('stepId') or '',r.get('materialId') or '',r['relativePath']):r for r in manifest['contentAssociations']}
        approval_id='USER_ORDER_2026_11_10_PROFELAB_VISUAL_INTEGRATION'
        approval_sha=sha(Path(approval_order).read_bytes())
        slots=set();runtime=[];files={};mapping=[]
        keep={'contentId','contentVersion','mediaId','role','stepId','materialId','mime','width','height','sha256','altText','caption','sourceType','licenseOrRights','stepFingerprint','mediaRevision'}
        for row in selection:
            content=row['contentId'];definition=definitions[content];role=row['role']
            assert row['contentVersion']==definition['contentVersion']
            assert role in {'catalog_cover','detail_hero','material_icon','step_photo','outcome_reference','teacher_reference'}
            slot=(content,role,row.get('stepId') or '',row.get('materialId') or '');assert slot not in slots;slots.add(slot)
            path=row['relativePath'];assert PurePosixPath(path).suffix=='.webp' and path in physical
            assert (*slot,path) in associations
            binary=z.read(path);file=physical[path];digest=sha(binary)
            assert binary[:4]==b'RIFF' and binary[8:12]==b'WEBP' and len(binary)<=8*1024*1024
            assert digest==row['sha256']==file['sha256'] and row['mime']==file['mime']=='image/webp'
            assert row['width']==file['width'] and row['height']==file['height'] and len(binary)==file['byteSize']
            assert row['altText'].strip() and len(row['altText'])<=600 and len(row.get('caption',''))<=1000
            if role=='step_photo':
                steps={s.get('id',f'step-{i}'):s for i,s in enumerate(definition['steps'])}
                step=steps[row['stepId']];fingerprint=sha(compact([step['title'],step.get('instructionNote','')]).encode())
                assert row['stepFingerprint']==fingerprint
            if role=='material_icon':assert row['materialId'] in materials
            filename='work_'+digest+'.webp'
            assert filename not in files or files[filename]==binary
            files[filename]=binary
            ref={k:v for k,v in row.items() if k in keep}
            ref.update(assetFilename=filename,byteSize=len(binary),reviewStatus='APPROVED_BY_USER',safetyReviewStatus='APPROVED_BY_USER',
                approvalId=approval_id,approvalScope='EDITORIAL_IMAGE_INTEGRATION_NOT_PHYSICAL_ACTIVITY_SAFETY',mediaRevision=row.get('mediaRevision',1))
            runtime.append(ref)
            source=associations[(*slot,path)]
            mapping.append(dict(contentId=content,contentVersion=ref['contentVersion'],role=role,stepId=row.get('stepId'),materialId=row.get('materialId'),
                sourceRelativePath=path,assetFilename=filename,sha256=digest,sourceReviewStatus=row['reviewStatus'],sourceSafetyReviewStatus=row['safetyReviewStatus'],
                sourceCommitAtGeneration=source.get('sourceCommitAtGeneration',source.get('sourceCommit')),validatedSourceCommit=manifest['validatedSourceCommit'],
                sourceType=row['sourceType'],licenseOrRights=row['licenseOrRights'],materialSafetyReview=source.get('materialSafetyReview'),
                usageNote=source.get('usageNote'),approvalId=approval_id,approvalOrderSha256=approval_sha,approvalScope=ref['approvalScope']))
        roles=collections.Counter(r['role'] for r in runtime)
        assert roles==dict(step_photo=80,material_icon=69,catalog_cover=20,detail_hero=20,outcome_reference=20,teacher_reference=20)
        assert len(files)==199 and {r['materialId'] for r in runtime if r['role']=='material_icon'}==materials
        for content in definitions:
            assert sum(r['contentId']==content and r['role']=='step_photo' for r in runtime)==len(definitions[content]['steps'])
        previous=json.loads((assets/'media_slots_v1.json').read_text(encoding='utf-8'))
        registry={**previous,'status':'APPROVED_BANK_INTEGRATED','bankVersion':'2026.11.10.1','approvalId':approval_id,'approvalOrderSha256':approval_sha,
            'approvalScope':'Editorial visual integration; activity safety and quantities still require teacher review.',
            'sourceManifestSha256':sha(original),'sourceBindingsSha256':sha(proposal),'sourceHead':manifest['validatedSourceCommit'],'resources':runtime}
        encoded=(json.dumps(registry,ensure_ascii=False,indent=2)+'\n').encode();assert len(encoded)<=2_000_000
        # Validate everything before the first write; preserve old PNGs and other catalog files.
        destination=assets/'images';destination.mkdir(parents=True,exist_ok=True)
        for name,binary in files.items():(destination/name).write_bytes(binary)
        (assets/'media_slots_v1.json').write_bytes(encoded)
        docs=project/'docs/taller/T3_0';docs.mkdir(parents=True,exist_ok=True)
        receipt=dict(schema='PROFELAB_VISUAL_INTEGRATION_1',sourceHead=manifest['validatedSourceCommit'],bankVersion=registry['bankVersion'],approvalId=approval_id,
            approvalOrderSha256=approval_sha,sourceManifestSha256=sha(original),sourceBindingsSha256=sha(proposal),
            note='Original review manifests remain unchanged. Explicit user approval authorizes these editorial images; this does not certify real materials, science outcomes or physical activity safety.',
            activities=20,steps=80,materialIdentities=45,materialBindings=69,bindings=len(runtime),uniqueWebpFiles=len(files),webpBytes=sum(map(len,files.values())),resources=mapping)
        (docs/'BANCO_VISUAL_INTEGRADO.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
        from modularize_work_media import modularize
        layout=modularize(project)
        return {**{k:v for k,v in receipt.items() if k!='resources'},'modularLayout':layout}

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--bank',type=Path,required=True);p.add_argument('--project',type=Path,required=True)
    p.add_argument('--approval-order',type=Path,required=True);p.add_argument('--user-approved',action='store_true',required=True)
    args=p.parse_args();print(json.dumps(integrate(args.bank,args.project,args.approval_order),ensure_ascii=False,indent=2))
