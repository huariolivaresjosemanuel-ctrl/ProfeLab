#!/usr/bin/env python3
"""Valida PDFs descargados por el usuario desde fuentes oficiales, NO descarga ni modifica sus bytes.
Uso: python validar_pdfs_minedu.py /ruta/a/los/pdfs
"""
import json,hashlib,sys
from pathlib import Path
BASE=Path(__file__).resolve().parents[1]
cat=json.loads((BASE/'06_FUENTES'/'CATALOGO_MINEDU_VERIFICADO.json').read_text(encoding='utf-8'))
folder=Path(sys.argv[1]).expanduser() if len(sys.argv)>1 else BASE/'09_PDF_MINEDU_DESCARGADOS'
results=[]
for doc in cat['docs']:
    p=folder/doc['filename']
    item={'id':doc['id'],'filename':doc['filename'],'exists':p.is_file(),'status':'NOT_FOUND'}
    if p.is_file():
        with p.open('rb') as f:head=f.read(1024)
        if not head.lstrip().startswith(b'%PDF-'):item['status']='NOT_PDF_SIGNATURE'
        else:
            h=hashlib.sha256();size=0
            with p.open('rb') as f:
                for b in iter(lambda:f.read(1024*1024),b''):
                    h.update(b);size+=len(b)
            item.update(status='VALID_PDF_HEADER_NEEDS_RIGHTS_CHECK',size=size,sha256=h.hexdigest())
            try:
                from pypdf import PdfReader
                item['pages']=len(PdfReader(str(p)).pages)
            except ImportError: item['pages']='NOT_CHECKED_INSTALL_PYPDF_OPTIONALLY'
            except Exception as e: item['pages']=f'PARSE_ERROR:{type(e).__name__}'
    results.append(item)
print(json.dumps({'folder':str(folder),'documents':results,'warning':'Integridad de PDF NO concede permiso de distribución en APK.'},ensure_ascii=False,indent=2))
