$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$jdk = if ($env:FIXEDLOCATION_JAVA_HOME) { $env:FIXEDLOCATION_JAVA_HOME } elseif (Test-Path 'D:\jdk24\bin\javac.exe') { 'D:\jdk24' } elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) { $env:JAVA_HOME } else { Split-Path -Parent (Split-Path -Parent (Get-Command javac).Source) }
$testOut = Join-Path $taskRoot 'build\tests'
New-Item -ItemType Directory -Force -Path $testOut | Out-Null
$sources = @("$taskRoot\tests\CoreTest.java", "$taskRoot\app\src\main\java\app\fixedlocation\Coordinates.java", "$taskRoot\app\src\main\java\app\fixedlocation\MockSession.java", "$taskRoot\app\src\main\java\app\fixedlocation\UpdateHealth.java")
& "$jdk\bin\javac.exe" --release 8 -Xlint:-options -encoding UTF-8 -d $testOut @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $testOut app.fixedlocation.CoreTest
exit $LASTEXITCODE
