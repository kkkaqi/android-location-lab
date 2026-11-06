"""Stage native build inputs under an ASCII-only cache path for Windows AIDL/NDK."""
from pathlib import Path
import os
import shutil

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(os.environ['USERPROFILE']) / '.codex/build-cache/fixed-location-container'

def main():
    source = ROOT / 'container-lab'
    project = CACHE / 'project'
    ignore = shutil.ignore_patterns('build', '.gradle', '.cxx', '.idea', 'bin', '__pycache__', 'local.properties')
    shutil.copytree(source, project, dirs_exist_ok=True, ignore=ignore)
    for folder in ('platforms/android-35', 'build-tools/35.0.0', 'ndk/28.2.13676358'):
        sdk_source = ROOT / '.tooling/sdk' / folder
        sdk_dest = CACHE / 'sdk' / folder
        if not (sdk_dest / '.verified').exists():
            print('Staging SDK:', folder, flush=True)
            shutil.copytree(sdk_source, sdk_dest, dirs_exist_ok=True)
    properties = 'sdk.dir=' + (CACHE / 'sdk').as_posix().replace(':', '\\:') + '\n'
    (project / 'local.properties').write_text(properties, encoding='ascii')
    print('Native build cache:', CACHE, flush=True)

if __name__ == '__main__': main()
