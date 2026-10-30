"""Lightweight delimiter/quoted-string audit; not a Kotlin compiler or parser."""
from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[1]
def audit(text):
    stack=[];i=0
    def string(i):
        if text.startswith('"""',i):
            j=text.find('"""',i+3)
            if j<0:raise ValueError('Unterminated raw string')
            return j+3
        quote=text[i];i+=1
        while i<len(text):
            if text[i]=='\\':i+=2;continue
            if quote=='"' and text.startswith('${',i):i=interpolation(i+2);continue
            if text[i]==quote:return i+1
            i+=1
        raise ValueError('Unterminated string')
    def interpolation(i):
        depth=1
        while i<len(text):
            if text[i] in '\"\'':i=string(i);continue
            if text[i]=='{':depth+=1
            if text[i]=='}':
                depth-=1
                if depth==0:return i+1
            i+=1
        raise ValueError('Unterminated interpolation')
    while i<len(text):
        if text.startswith('//',i):
            j=text.find('\n',i);i=len(text) if j<0 else j;continue
        if text.startswith('/*',i):
            j=text.find('*/',i+2)
            if j<0:raise ValueError('Unterminated comment')
            i=j+2;continue
        c=text[i]
        if c in '\"\'':i=string(i);continue
        if c in '([{':stack.append((c,i))
        if c in ')]}':
            if not stack or stack[-1][0]!={')':'(',']':'[','}':'{'}[c]:raise ValueError('Unbalanced delimiter at '+str(i))
            stack.pop()
        i+=1
    if stack:raise ValueError('Open delimiter '+str(stack[-1]))
errors=[];files=list(ROOT.rglob('*.kt'))+list(ROOT.rglob('*.kts'))
for p in files:
    try:audit(p.read_text(encoding='utf-8'))
    except ValueError as e:errors.append({'file':p.relative_to(ROOT).as_posix(),'error':str(e)})
report={'scope':'Delimiters only; not Kotlin compilation','files':len(files),'errors':errors}
output=ROOT/'docs/qa/completo/delimitadores.json';output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps(report,indent=2))
sys.exit(bool(errors))
