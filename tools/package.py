"""Package source only, excluding SDKs, generated files and signing keys."""
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'dist' / 'fixed-location-source.zip'
ROOT_FILES = ['.gitignore', 'PRODUCT.md', 'DESIGN.md', 'README.md', 'settings.gradle', 'build.gradle', 'build.ps1']

def main():
    files = [ROOT / name for name in ROOT_FILES]
    for folder in ('app', 'tools', 'tests', 'docs'):
        files += [p for p in (ROOT / folder).rglob('*') if p.is_file()
                  and '__pycache__' not in p.parts and 'build' not in p.parts]
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(OUTPUT, 'w', zipfile.ZIP_DEFLATED) as archive:
        for file in sorted(files):
            archive.write(file, file.relative_to(ROOT).as_posix())
    with zipfile.ZipFile(OUTPUT) as archive:
        assert archive.testzip() is None
        assert not any(name.endswith(('.p12', '.jks', '.keystore', '.pyc')) for name in archive.namelist())
        print('Source package verified:', len(archive.namelist()), 'files,', OUTPUT.stat().st_size, 'bytes')

if __name__ == '__main__':
    main()
