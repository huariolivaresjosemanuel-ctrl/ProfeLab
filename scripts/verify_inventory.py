"""Verify delivered bytes before modifying them. Standard library only."""
from pathlib import Path
import hashlib, sys

ROOT=Path(__file__).resolve().parents[1]
errors=[]
count=0
listed=set()
for line in (ROOT/'SHA256SUMS.txt').read_text(encoding='utf-8').splitlines():
    digest,relative=line.split('  ',1)
    target=(ROOT/relative).resolve()
    if not target.is_relative_to(ROOT.resolve()): errors.append(relative+': unsafe path');continue
    listed.add(relative);count+=1
    if not target.is_file() or hashlib.sha256(target.read_bytes()).hexdigest()!=digest: errors.append(relative)
print(f'{count-len(errors)}/{count} hashes match')
for error in errors: print('MISMATCH',error)
actual={p.relative_to(ROOT).as_posix() for p in ROOT.rglob('*') if p.is_file() and not any(part in {'.git','.gradle','build','__pycache__','.idea'} for part in p.relative_to(ROOT).parts) and p.name!='local.properties' and p.suffix not in {'.pyc','.apk','.aab'}}
unexpected=actual-listed-{'SHA256SUMS.txt'}
for path in sorted(unexpected):print('UNLISTED',path)
errors.extend(unexpected)
sys.exit(bool(errors))
