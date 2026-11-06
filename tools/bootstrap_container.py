"""Install the pinned official NDK locally for the ARM64 container experiment."""
from pathlib import Path
import hashlib
import os
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NAME = 'android-ndk-r28c-windows.zip'
SHA1 = '086bba43ff2f5eb0e387b15c8278bb4e0d89ba1d'
DEST = ROOT / '.tooling/sdk/ndk/28.2.13676358'

def main():
    if (DEST / '.verified').exists():
        print('NDK ready:', DEST, flush=True)
        return
    archive = ROOT / '.tooling' / NAME
    if not archive.exists():
        partial = archive.with_suffix('.partial')
        with urllib.request.urlopen('https://dl.google.com/android/repository/' + NAME, timeout=60) as response, partial.open('wb') as output:
            total = 0
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk: break
                output.write(chunk); total += len(chunk)
                if total % (50 * 1024 * 1024) == 0: print('NDK downloaded MiB:', total // (1024 * 1024), flush=True)
        os.replace(partial, archive)
    digest = hashlib.sha1()
    with archive.open('rb') as input_file:
        for chunk in iter(lambda: input_file.read(1024 * 1024), b''): digest.update(chunk)
    if digest.hexdigest() != SHA1: raise RuntimeError('NDK checksum mismatch')
    print('NDK checksum verified; extracting', flush=True)
    with zipfile.ZipFile(archive) as z:
        for info in z.infolist():
            if info.is_dir(): continue
            parts = Path(info.filename).parts[1:]
            if not parts: continue
            target = DEST.joinpath(*parts).resolve()
            if not target.is_relative_to(DEST.resolve()): raise ValueError('Unsafe archive path')
            target.parent.mkdir(parents=True, exist_ok=True)
            with z.open(info) as source, target.open('wb') as output:
                for chunk in iter(lambda: source.read(1024 * 1024), b''): output.write(chunk)
    (DEST / '.verified').write_text(SHA1)
    print('NDK ready:', DEST, flush=True)

if __name__ == '__main__': main()
