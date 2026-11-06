"""Build a signed test APK using official Android SDK tools, without global installs."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SDK = ROOT / '.tooling' / 'sdk'
TOOLS = SDK / 'build-tools' / '35.0.0'
ANDROID = SDK / 'platforms' / 'android-35' / 'android.jar'
OUT = ROOT / 'build' / 'apk'
DIST = ROOT / 'dist'


def jdk_home():
    candidates = [os.environ.get('FIXEDLOCATION_JAVA_HOME'), r'D:\jdk24', os.environ.get('JAVA_HOME')]
    if shutil.which('javac'):
        candidates.append(str(Path(shutil.which('javac')).parent.parent))
    for candidate in candidates:
        if not candidate:
            continue
        java = Path(candidate) / 'bin' / 'java.exe'
        if not java.exists():
            continue
        version = subprocess.run([str(java), '-version'], capture_output=True, text=True)
        if 'version "1.' not in version.stderr:
            return Path(candidate)
    raise RuntimeError('JDK 17 or newer is required. Set FIXEDLOCATION_JAVA_HOME.')


def main():
    if not ANDROID.exists():
        raise RuntimeError('Run python tools/bootstrap.py first.')
    jdk = jdk_home()
    env = dict(os.environ, JAVA_HOME=str(jdk))
    env['PATH'] = str(jdk / 'bin') + os.pathsep + env.get('PATH', '')

    def run(*args):
        # aapt2 on Windows cannot open absolute paths with non-ASCII characters.
        # Relative paths keep both the workspace and build portable.
        command = [str(arg.relative_to(ROOT)) if isinstance(arg, Path) and arg.is_relative_to(ROOT) else str(arg) for arg in args]
        print('> ' + Path(command[0]).name, flush=True)
        subprocess.run(command, cwd=ROOT, env=env, check=True)

    for folder in (OUT, OUT / 'generated', OUT / 'classes', OUT / 'dex', DIST):
        folder.mkdir(parents=True, exist_ok=True)
    # AGP 8 reads namespace from Gradle; aapt2's standalone pipeline needs package.
    ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
    manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml')
    manifest.getroot().set('package', 'app.fixedlocation')
    manifest.write(OUT / 'AndroidManifest.xml', encoding='utf-8', xml_declaration=True)
    run(TOOLS / 'aapt2.exe', 'compile', '--dir', ROOT / 'app/src/main/res', '-o', OUT / 'resources.zip')
    run(TOOLS / 'aapt2.exe', 'link', '-o', OUT / 'resources.apk', '-I', ANDROID,
        '--manifest', OUT / 'AndroidManifest.xml', '--java', OUT / 'generated',
        '--min-sdk-version', '31', '--target-sdk-version', '35', OUT / 'resources.zip')
    sources = sorted((ROOT / 'app/src/main/java').rglob('*.java')) + sorted((OUT / 'generated').rglob('*.java'))
    # javac argfile avoids Windows command-line size and preserves Unicode paths.
    args = ['--release', '8', '-Xlint:-options', '-encoding', 'UTF-8', '-classpath', str(ANDROID.relative_to(ROOT)), '-d', str((OUT / 'classes').relative_to(ROOT))]
    args += [str(file.relative_to(ROOT)) for file in sources]
    argfile = OUT / 'javac.args'
    argfile.write_text('\n'.join('"' + value.replace('\\', '/') + '"' for value in args), encoding='utf-8')
    run(jdk / 'bin/javac.exe', '@' + str(argfile.relative_to(ROOT)))
    with zipfile.ZipFile(OUT / 'classes.jar', 'w', zipfile.ZIP_DEFLATED) as jar:
        for file in sorted((OUT / 'classes').rglob('*.class')):
            jar.write(file, file.relative_to(OUT / 'classes').as_posix())
    run(jdk / 'bin/java.exe', '-cp', TOOLS / 'lib/d8.jar', 'com.android.tools.r8.D8',
        '--min-api', '31', '--lib', ANDROID, '--output', OUT / 'dex', OUT / 'classes.jar')
    shutil.copyfile(OUT / 'resources.apk', OUT / 'unsigned.apk')
    with zipfile.ZipFile(OUT / 'unsigned.apk', 'a', zipfile.ZIP_DEFLATED) as apk:
        for dex in sorted((OUT / 'dex').glob('*.dex')):
            apk.write(dex, dex.name)
    run(TOOLS / 'zipalign.exe', '-f', '-p', '4', OUT / 'unsigned.apk', OUT / 'aligned.apk')
    keystore = ROOT / '.tooling' / 'fixedlocation-test.p12'
    if not keystore.exists():
        run(jdk / 'bin/keytool.exe', '-genkeypair', '-keystore', keystore,
            '-storetype', 'PKCS12', '-storepass', 'android', '-keypass', 'android',
            '-alias', 'fixedlocation-test', '-keyalg', 'RSA', '-keysize', '2048',
            '-validity', '3650', '-dname', 'CN=Fixed Location Local Test, O=Local Development, C=CN')
    version = manifest.getroot().get('{http://schemas.android.com/apk/res/android}versionName')
    apk = DIST / ('fixed-location-' + version + '.apk')
    run(jdk / 'bin/java.exe', '-jar', TOOLS / 'lib/apksigner.jar', 'sign',
        '--ks', keystore, '--ks-key-alias', 'fixedlocation-test', '--ks-pass', 'pass:android',
        '--out', apk, OUT / 'aligned.apk')
    run(jdk / 'bin/java.exe', '-jar', TOOLS / 'lib/apksigner.jar', 'verify', '--verbose', '--print-certs', apk)
    run(TOOLS / 'zipalign.exe', '-c', '4', apk)
    result = subprocess.run([str(TOOLS / 'aapt.exe'), 'dump', 'badging', str(apk.relative_to(ROOT))], capture_output=True, cwd=ROOT, env=env, check=True)
    (DIST / 'apk-info.txt').write_bytes(result.stdout)
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    (DIST / 'SHA256SUMS.txt').write_text(digest + '  ' + apk.name + '\n', encoding='ascii')
    print(json.dumps({'apk': str(apk), 'bytes': apk.stat().st_size, 'sha256': digest}, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    main()
