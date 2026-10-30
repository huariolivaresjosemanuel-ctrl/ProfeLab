#!/usr/bin/env python3
"""Catalogador de PDFs oficiales MINEDU. Utilidad de desarrollo, NO empaquetador automático en APK.
Uso: python 07_SCRIPTS/descargar_minedu.py --descargar
     python 07_SCRIPTS/descargar_minedu.py --validar-directorio <carpeta>
Requiere red solo para --descargar, no el uso Android. Nunca sortea desafíos de seguridad ni adivina enlaces.
"""
import argparse,hashlib,json,os,pathlib,urllib.request,urllib.parse,html.parser,datetime
ROOT=pathlib.Path(__file__).resolve().parent.parent
CAT=json.loads((ROOT/'06_FUENTES/CATALOGO_MINEDU_VERIFICADO.json').read_text(encoding='utf8'))['docs']
class Links(html.parser.HTMLParser):
 def __init__(self):super().__init__();self.links=[]
 def handle_starttag(self,t,a):
  if t=='a':
   d=dict(a);link=d.get('href','')
   if ('.pdf' in link.lower() or '/bitstream/' in link):self.links.append(link)
def read_url(url, limit=48*1024*1024):
 req=urllib.request.Request(url,headers={'User-Agent':'ProfeLab-Research/1.0 (offline library preparation)'})
 with urllib.request.urlopen(req,timeout=30) as res:
  if not url.startswith('https://'):raise ValueError('URL sin HTTPS')
  data=res.read(limit+1)
  if len(data)>limit:raise ValueError('Archivo supera límite 48MB')
  return data, res.geturl()
def official_pdf_link(doc):
 if doc.get('official_pdf_url'):return doc['official_pdf_url']
 page=doc['official_page'];raw,final=read_url(page,3*1024*1024)
 p=Links();p.feed(raw.decode('utf8','replace'))
 for link in p.links:
  url=urllib.parse.urljoin(final,link)
  if urllib.parse.urlparse(url).hostname=='repositorio.minedu.gob.pe' and ('bitstream' in url or '.pdf' in url.lower()):return url
 raise RuntimeError('La ficha no mostró un enlace al PDF verificable. Descargue desde el navegador oficial.')
def status(file,doc,source='local'):
 b=file.read_bytes() if file.exists() else b''
 if not b.startswith(b'%PDF-') or b'%%EOF' not in b[-8192:]:raise ValueError('No es un PDF válido o está incompleto')
 return {'id':doc['id'],'title':doc['title'],'status':'ARCHIVO_VERIFICADO_SIN_AUDITORIA_DE_DERECHOS','path':str(file),'size':len(b),'sha256':hashlib.sha256(b).hexdigest(),'source':source,'canBundleInCommercialApk':False,'licenseReviewRequired':True}
def main():
 a=argparse.ArgumentParser();a.add_argument('--descargar',action='store_true');a.add_argument('--validar-directorio');a.add_argument('--incluir-referencias-restringidas',action='store_true');args=a.parse_args()
 if not args.descargar and not args.validar_directorio:a.error('Elija --descargar o --validar-directorio')
 out=pathlib.Path(args.validar_directorio) if args.validar_directorio else ROOT/'09_PDF_MINEDU_DESCARGADOS';out.mkdir(parents=True,exist_ok=True)
 result=[]
 for doc in CAT:
  rec={'id':doc['id'],'title':doc['title'],'status':'PENDIENTE','canBundleInCommercialApk':False,'licenseReviewRequired':True}
  file=out/doc['filename']
  if doc['bundling_policy']=='NO_BUNDLE_REFERENCE_ONLY' and not args.incluir_referencias_restringidas:
   rec['status']='SOLO_REFERENCIA_NO_DESCARGAR_POR_DEFECTO';result.append(rec);continue
  try:
   if args.descargar and not file.exists():
    url=official_pdf_link(doc);data,final=read_url(url)
    if not data.startswith(b'%PDF-'):raise ValueError('El sitio devolvió HTML, bloqueo anti-bot u otro contenido, no PDF')
    temp=file.with_suffix('.pdf.partial');temp.write_bytes(data)
    v=status(temp,doc,final);os.replace(temp,file)
   rec=status(file,doc,'archivo local' if args.validar_directorio else doc['official_page'])
  except Exception as ex:rec['status']='ERROR_O_PENDIENTE';rec['detail']=str(ex);rec['instruccion']='Descargar manualmente desde la URL official_page y guardar como '+doc['filename']
  result.append(rec);print(doc['id'],rec['status'])
 manifest={'createdAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'catalogSchemaVersion':1,'files':result,'warning':'No incluir PDFs en APK sin autorización y atribución verificadas'}
 path=out/'ESTADO_DESCARGAS_MINEDU.json';path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf8')
 print('INFORME:',path,'VALIDADOS:',sum(1 for r in result if r['status'].startswith('ARCHIVO_VERIFICADO')),'/',len(result))
if __name__=='__main__': main()
