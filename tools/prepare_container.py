"""Prepare a pinned, attributed core fork without the upstream automatic uploader."""
from pathlib import Path
import shutil
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / '.tooling/research/blackbox-android-main'
DEST = ROOT / 'container-lab/vendor/blackbox'

def replace_body(text, signature, body):
    offset = text.index(signature)
    start = text.index('{', offset)
    pattern = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|/\*.*?\*/|//[^\n]*'
    masked = re.sub(pattern, lambda match: ' ' * len(match.group()), text, flags=re.S)
    depth = 1
    end = start + 1
    while depth:
        if masked[end] == '{': depth += 1
        elif masked[end] == '}': depth -= 1
        end += 1
    return text[:start + 1] + '\n' + body + '\n    }' + text[end:]

def main():
    if DEST.exists():
        raise RuntimeError('Vendored core already exists; build it directly. Refusing to overwrite local compatibility fixes.')
    expected = 'bf1623b7d45157bbc87be46a29352dd6bbbde9fa'
    if (SOURCE / 'UPSTREAM_COMMIT.txt').read_text().strip() != expected:
        raise RuntimeError('Unexpected upstream revision')
    for name in ('Bcore', 'black-reflection', 'compiler'):
        shutil.copytree(SOURCE / name, DEST / name, dirs_exist_ok=True)
    for name in ('LICENSE', 'README.md', 'UPSTREAM_COMMIT.txt'):
        shutil.copyfile(SOURCE / name, DEST / name)
    java = DEST / 'Bcore/src/main/java/com/tegufy/blackbox'
    core = java / 'BlackBoxCore.java'
    text = core.read_text(encoding='utf-8')
    text = text.replace('import com.tegufy.blackbox.utils.LogSender;', '')
    text = text.replace('            startLogcat();', '            // Local fork: automatic logcat capture removed.')
    text = replace_body(text, 'private void startLogcat()', '        // Local fork: no public-storage logcat capture or logcat clearing.')
    text = replace_body(text, 'public void sendLogs(String caption, boolean async, LogSendListener listener)',
                        '        // Local fork: automatic upload and log collection are disabled.\n'
                        '        if (listener != null) listener.onFailure("Automatic upload disabled in container lab");')
    core.write_text(text, encoding='utf-8')
    sender = java / 'utils/LogSender.java'
    if sender.exists(): sender.unlink()
    config = java / 'app/configuration/ClientConfiguration.java'
    config.write_text(config.read_text(encoding='utf-8').replace('return "-1003719573856";', 'return "";'), encoding='utf-8')
    compat = java / 'utils/compat/BuildCompat.java'
    text = compat.read_text(encoding='utf-8')
    text = replace_body(text, 'public static boolean isU()', '        return Build.VERSION.SDK_INT >= 34;')
    text = replace_body(text, 'public static boolean isTiramisu()', '        return Build.VERSION.SDK_INT >= 33;')
    compat.write_text(text, encoding='utf-8')
    build = DEST / 'Bcore/build.gradle'
    text = build.read_text(encoding='utf-8').replace("abiFilters 'arm64-v8a' , 'armeabi-v7a'", "abiFilters 'arm64-v8a'")
    text = text.replace('JavaVersion.VERSION_21', 'JavaVersion.VERSION_17')
    build.write_text(text, encoding='utf-8')
    with build.open('a', encoding='utf-8') as output:
        output.write('''
// Local Windows AIDL comment fix; only generated command comments are removed.
tasks.withType(JavaCompile).configureEach {
    doFirst {
        fileTree("$buildDir/generated/aidl_source_output_dir").matching {
            include '**/*.java'
        }.files.each { generated ->
            generated.setText(generated.readLines('UTF-8').findAll {
                !it.startsWith(' * Using:')
            }.join(System.lineSeparator()), 'UTF-8')
        }
    }
}
''')
    for name, tag in (('ProxyActivity.java', 'ProxyActivity'), ('ProxyPendingActivity.java', 'ProxyPendingActivity')):
        path = java / 'proxy' / name
        text = path.read_text(encoding='utf-8')
        text = replace_body(text, 'protected void onCreate(@Nullable Bundle savedInstanceState)',
                            '        super.onCreate(savedInstanceState);\n'
                            '        android.util.Log.e("' + tag + '", "Raw target fallback disabled in container lab");\n'
                            '        finish();')
        path.write_text(text, encoding='utf-8')
    common = java / 'fake/service/ActivityManagerCommonProxy.java'
    text = common.read_text(encoding='utf-8')
    anchor = '                    intent.setPackage(origPackage);\n                    return method.invoke(who, args);'
    replacement = '''                    intent.setPackage(origPackage);
                    if ("com.tencent.mm".equals(intent.getPackage())
                            || (intent.getComponent() != null && "com.tencent.mm".equals(intent.getComponent().getPackageName()))) {
                        throw new IllegalStateException("Container resolution failed; real WeChat fallback disabled");
                    }
                    return method.invoke(who, args);'''
    if anchor not in text: raise RuntimeError('Upstream fallback changed')
    common.write_text(text.replace(anchor, replacement), encoding='utf-8')
    # The upstream NDK makefile is already 16-KB-page aligned. Pin both modules to r28c.
    print('Prepared pinned ARM64 BlackBox fork:', DEST, flush=True)
    for file in (DEST / 'Bcore/src/main').rglob('*'):
        if file.is_file() and file.suffix in ('.java', '.cpp', '.c', '.h'):
            if b'logs-sender-api.vercel.app' in file.read_bytes():
                raise RuntimeError('Unexpected remaining upload endpoint: ' + str(file))
    print('Verified: automatic upload endpoint removed', flush=True)

if __name__ == '__main__': main()
