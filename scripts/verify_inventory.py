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
sys.exit(bool(errors))
