"""Release helper. No key generation; no credentials printed. Standard library only."""
import argparse, base64, binascii, hashlib, json, os, pathlib, re, shutil, subprocess

def required(env, name):
    value=env.get(name,'')
    if not value.strip(): raise ValueError('Falta configuración: '+name)
    return value

def fingerprint(value):
    normalized=value.replace(':','').strip().lower()
    if not re.fullmatch('[0-9a-f]{64}', normalized): raise ValueError('Huella SHA-256 de certificado inválida')
    return normalized

def directory(env):
    root=pathlib.Path(required(env,'RUNNER_TEMP')).resolve()
    folder=root/'profelab-signing'
    if folder.is_symlink() or (folder.exists() and folder.resolve().parent != root):
        raise ValueError('Directorio temporal inseguro')
    return folder

def prepare(env):
    for name in ['PROFELAB_KEYSTORE_PASSWORD','PROFELAB_KEY_ALIAS','PROFELAB_KEY_PASSWORD']:
        required(env,name)
    fingerprint(required(env,'PROFELAB_SIGNING_CERT_SHA256'))
    version=int(required(env,'PROFELAB_VERSION_CODE'))
    previous=int(required(env,'PROFELAB_LAST_DISTRIBUTED_VERSION_CODE'))
    if not 2 <= version <= 2100000000 or version <= previous:
        raise ValueError('versionCode debe superar la última versión distribuida')
    encoded=required(env,'PROFELAB_KEYSTORE_BASE64')
    if len(encoded)>4*1024*1024: raise ValueError('Keystore demasiado grande')
    try: data=base64.b64decode(encoded,validate=True)
    except (binascii.Error,ValueError): raise ValueError('Keystore Base64 inválido') from None
    if not data: raise ValueError('Keystore vacío')
    folder=directory(env)
    if folder.exists(): raise ValueError('Ya existe el directorio temporal de firma')
    folder.mkdir(mode=0o700)
    path=folder/'release.keystore'
    try:
        with path.open('xb') as output: output.write(data)
        path.chmod(0o600)
    except BaseException:
        shutil.rmtree(folder)
        raise
    return path

def cleanup(env):
    folder=directory(env)
    if folder.exists(): shutil.rmtree(folder)

def verify(env, apk, output, run=subprocess.run):
    apk=pathlib.Path(apk); output=pathlib.Path(output)
    expected=fingerprint(required(env,'PROFELAB_SIGNING_CERT_SHA256'))
    result=run([required(env,'PROFELAB_APKSIGNER'),'verify','--verbose','--print-certs',str(apk)],
               capture_output=True,text=True,check=False)
    if result.returncode: raise ValueError('apksigner rechazó la firma; no se publica la APK')
    actual=re.findall(r'^Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F:]+)\s*$',result.stdout,re.M)
    if len(actual)!=1 or fingerprint(actual[0])!=expected:
        raise ValueError('El certificado APK no coincide con la huella permanente configurada')
    if not re.search(r'Verified using v2 scheme.*:\s*true',result.stdout):
        raise ValueError('Se requiere firma v2 para la compatibilidad API26')
    metadata=run([required(env,'PROFELAB_AAPT'),'dump','badging',str(apk)],capture_output=True,text=True,check=False)
    expected_code=required(env,'PROFELAB_VERSION_CODE')
    package=re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",metadata.stdout,re.M)
    if metadata.returncode or not package or package.groups()!=('pe.profelab.app',expected_code,'0.1.1-h1.1'):
        raise ValueError('Identificador o versión de la APK no coincide con H1.1')
    output.mkdir(parents=True,exist_ok=False)
    destination=output/'ProfeLab-H1.1-release.apk'
    shutil.copyfile(apk,destination)
    digest=hashlib.sha256(destination.read_bytes()).hexdigest()
    (output/'SHA256SUMS.txt').write_text(digest+'  '+destination.name+'\n',encoding='utf-8')
    (output/'firma-publica.json').write_text(json.dumps({'certificate_sha256':expected,
        'apk_sha256':digest,'versionCode':int(required(env,'PROFELAB_VERSION_CODE')),
        'versionName':'0.1.1-h1.1','signature_verified':True},indent=2),encoding='utf-8')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['prepare','cleanup','verify'])
    parser.add_argument('--apk');parser.add_argument('--output');args=parser.parse_args()
    try:
        if args.operation=='prepare': prepare(os.environ)
        elif args.operation=='cleanup': cleanup(os.environ)
        else: verify(os.environ,args.apk,args.output)
    except (ValueError,OSError) as error:
        # Error messages contain configuration names only; never tool stderr or secret values.
        raise SystemExit(str(error))
