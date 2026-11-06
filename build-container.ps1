param([string]$GradleHome = 'C:\Users\31776\.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3')
$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$jdk = if ($env:FIXEDLOCATION_JAVA_HOME) { $env:FIXEDLOCATION_JAVA_HOME } elseif (Test-Path 'D:\jdk24\bin\java.exe') { 'D:\jdk24' } else { $env:JAVA_HOME }
if (!(Test-Path "$GradleHome\bin\gradle.bat")) { throw 'Set GradleHome to a local Gradle 8.14.3 installation.' }
if (!(Test-Path "$jdk\bin\java.exe")) { throw 'Set FIXEDLOCATION_JAVA_HOME to JDK 17+.' }
python "$taskRoot\tools\stage_container.py"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$buildCache = Join-Path $env:USERPROFILE '.codex\build-cache\fixed-location-container'
$env:JAVA_HOME = $jdk
$env:ANDROID_HOME = Join-Path $buildCache 'sdk'
& "$GradleHome\bin\gradle.bat" -p "$buildCache\project" --no-daemon --console=plain :app:assembleDebug
exit $LASTEXITCODE
