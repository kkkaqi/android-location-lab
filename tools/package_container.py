"""Verify and package the independent ARM64 container experiment and its source."""
from pathlib import Path
import hashlib
import os
import shutil
import subprocess
import zipfile
import re
from build import jdk_home

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(os.environ['USERPROFILE']) / '.codex/build-cache/fixed-location-container'
TOOLS = ROOT / '.tooling/sdk/build-tools/35.0.0'

def main():
    source = CACHE / 'project/app/build/outputs/apk/debug/app-debug.apk'
    if not source.exists(): raise RuntimeError('Build the container APK first.')
    source_metadata = subprocess.check_output([str(TOOLS / 'aapt.exe'), 'dump', 'badging', str(source)], cwd=ROOT)
    version = re.search(rb"versionName='([^']+)'", source_metadata).group(1).decode('ascii')
    destination = ROOT / ('dist/wechat-container-lab-' + version + '.apk')
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, destination)
    jdk = jdk_home()
    subprocess.run([str(jdk / 'bin/java.exe'), '-jar', str(TOOLS / 'lib/apksigner.jar'),
                    'verify', '--verbose', '--print-certs', str(destination)], cwd=ROOT, check=True)
    metadata = subprocess.check_output([str(TOOLS / 'aapt.exe'), 'dump', 'badging', str(destination.relative_to(ROOT))], cwd=ROOT)
    if b"name='app.containerlab'" not in metadata: raise RuntimeError('Unexpected application package')
    (ROOT / 'dist/container-apk-info.txt').write_bytes(metadata)
    with zipfile.ZipFile(destination) as z:
        libs = [name for name in z.namelist() if name.startswith('lib/') and name.endswith('.so')]
        if 'lib/arm64-v8a/libblackbox.so' not in libs: raise RuntimeError('Missing ARM64 virtualization engine')
        for name in libs:
            data = z.read(name)
            if not name.startswith('lib/arm64-v8a/') or data[:4] != b'\x7fELF' or data[4] != 2 or int.from_bytes(data[18:20], 'little') != 183:
                raise RuntimeError('Unexpected native architecture: ' + name)
        if 'assets/licenses/blackbox-Apache-2.0.txt' not in z.namelist(): raise RuntimeError('Missing attribution asset')
        for name in z.namelist():
            if name.endswith('.dex') and b'logs-sender-api.vercel.app' in z.read(name):
                raise RuntimeError('Removed log-upload endpoint remains in APK')
        print('Verified ARM64 ELF libraries:', libs)
    digest = hashlib.sha256(destination.read_bytes()).hexdigest()
    (ROOT / 'dist/container-SHA256SUMS.txt').write_text(digest + '  ' + destination.name + '\n', encoding='ascii')
    output = ROOT / 'dist/wechat-container-lab-source.zip'
    excluded = {'build', '.gradle', '.cxx', '.idea', 'bin', '__pycache__'}
    files = [p for p in (ROOT / 'container-lab').rglob('*') if p.is_file()
             and not excluded.intersection(p.relative_to(ROOT / 'container-lab').parts)
             and p.name != 'local.properties']
    files += [ROOT / 'build-container.ps1']
    files += [ROOT / 'tools' / name for name in ['bootstrap.py', 'bootstrap_container.py', 'prepare_container.py', 'stage_container.py', 'package_container.py', 'build.py']]
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as z:
        for file in sorted(files): z.write(file, file.relative_to(ROOT).as_posix())
    with zipfile.ZipFile(output) as z:
        if z.testzip() is not None: raise RuntimeError('Source archive corrupt')
        if any(name.endswith(('.p12', '.jks', '.keystore', '.class')) for name in z.namelist()):
            raise RuntimeError('Unexpected generated/private file in source archive')
    print('APK:', destination.name, destination.stat().st_size, 'bytes; SHA256', digest)
    print('Source archive:', len(files), 'files,', output.stat().st_size, 'bytes')

if __name__ == '__main__': main()
