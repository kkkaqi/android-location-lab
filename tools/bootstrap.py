"""Fetch pinned Android SDK archives from Google; verify before extracting."""
import concurrent.futures
import hashlib
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SDK = ROOT / '.tooling' / 'sdk'
PACKAGES = [
    ('platform-35_r02.zip', '0bb560a90a7a2cbd0dd8348224d518b638fe7949', 'android-35', 'platforms/android-35'),
    ('build-tools_r35_windows.zip', 'af059bb67cf7786f45ee0db85e2d24985df1b4b6', 'android-15', 'build-tools/35.0.0'),
    ('platform-tools_r37.0.1-win.zip', 'e03e78b1d80b396f1c3358e31251cb31740e1110', 'platform-tools', 'platform-tools'),
]

def fetch(spec):
    filename, checksum, prefix, destination = spec
    output = SDK / destination
    if (output / '.verified').exists():
        return str(output)
    cache = ROOT / '.tooling' / filename
    cache.parent.mkdir(parents=True, exist_ok=True)
    if not cache.exists():
        print('Downloading ' + filename, flush=True)
        urllib.request.urlretrieve('https://dl.google.com/android/repository/' + filename, cache)
    if hashlib.sha1(cache.read_bytes()).hexdigest() != checksum:
        raise RuntimeError('SDK archive checksum mismatch: ' + filename)
    with zipfile.ZipFile(cache) as archive:
        # Discover the archive root; distributions may use version labels.
        top = archive.namelist()[0].split('/')[0]
        for info in archive.infolist():
            parts = Path(info.filename).parts
            if len(parts) < 2 or info.is_dir():
                continue
            target = output.joinpath(*parts[1:]).resolve()
            if not target.is_relative_to(output.resolve()):
                raise RuntimeError('Unsafe archive path')
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(info))
    (output / '.verified').write_text(checksum)
    return str(output)

if __name__ == '__main__':
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        for result in pool.map(fetch, PACKAGES):
            print(result, flush=True)
