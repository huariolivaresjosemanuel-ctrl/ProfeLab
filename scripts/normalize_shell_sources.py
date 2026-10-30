"""Canonical bytes for the repository's shell entry points; no Android/device operations."""
from pathlib import Path
import argparse

def canonical_shell_bytes(data:bytes)->bytes:
    if data.startswith(b'\xef\xbb\xbf'):data=data[3:]
    data=data.replace(b'\r\n',b'\n')
    if b'\r' in data:raise ValueError('Bare CR in shell source; review the file instead of guessing its meaning')
    return data

def shell_paths(root:Path):
    wrapper=root/'gradlew'
    if wrapper.is_file():yield wrapper
    yield from sorted((root/'scripts').glob('*.sh'))

def normalize_repo(root:Path,check:bool=False)->list[str]:
    changed=[]
    for path in shell_paths(root):
        if path.is_symlink():raise ValueError('Shell source must not be a symlink: '+str(path))
        before=path.read_bytes();after=canonical_shell_bytes(before)
        if before!=after:
            changed.append(path.relative_to(root).as_posix())
            if not check:path.write_bytes(after)
    return changed

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1])
    parser.add_argument('--check',action='store_true')
    args=parser.parse_args();changed=normalize_repo(args.root.resolve(),args.check)
    for name in changed:print(('NEEDS_LF ' if args.check else 'NORMALIZED_LF ')+name)
    if args.check and changed:raise SystemExit(1)
    print('Shell byte check: LF, no BOM, no bare CR')
