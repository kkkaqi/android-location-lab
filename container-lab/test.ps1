$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$jdk = if ($env:FIXEDLOCATION_JAVA_HOME) { $env:FIXEDLOCATION_JAVA_HOME } elseif (Test-Path 'D:\jdk24\bin\javac.exe') { 'D:\jdk24' } else { $env:JAVA_HOME }
$out = Join-Path $taskRoot 'build\tests'
New-Item -ItemType Directory -Force -Path $out | Out-Null
& "$jdk\bin\javac.exe" --release 8 -encoding UTF-8 -d $out "$taskRoot\tests\RendererCompatPolicyTest.java" "$taskRoot\app\src\main\java\app\containerlab\RendererCompatPolicy.java"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.RendererCompatPolicyTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$androidJar = Join-Path $taskRoot '..\.tooling\sdk\platforms\android-35\android.jar'
if (!(Test-Path $androidJar)) { $androidJar = Join-Path $env:ANDROID_HOME 'platforms\android-35\android.jar' }
& "$jdk\bin\javac.exe" --release 8 -Xlint:-options -encoding UTF-8 -cp $androidJar -d $out "$taskRoot\tests\NullServiceRestartTest.java" "$taskRoot\tests\stubs\com\tegufy\blackbox\utils\compat\BundleCompat.java" "$taskRoot\vendor\blackbox\Bcore\src\main\java\com\tegufy\blackbox\proxy\record\ProxyServiceRecord.java"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp "$out;$androidJar" app.containerlab.NullServiceRestartTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\javac.exe" --release 8 -Xlint:-options -encoding UTF-8 -d $out "$taskRoot\tests\LocationUpdateBudgetTest.java" "$taskRoot\vendor\blackbox\Bcore\src\main\java\com\tegufy\blackbox\utils\compat\LocationUpdateBudget.java"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.LocationUpdateBudgetTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\javac.exe" --release 8 -Xlint:-options -encoding UTF-8 -d $out "$taskRoot\tests\ScopedLocationPolicyTest.java" "$taskRoot\vendor\blackbox\Bcore\src\main\java\com\tegufy\blackbox\utils\compat\ScopedLocationPolicy.java"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.ScopedLocationPolicyTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\javac.exe" --release 8 -Xlint:-options -encoding UTF-8 -d $out "$taskRoot\tests\LocaleQueryPolicyTest.java" "$taskRoot\vendor\blackbox\Bcore\src\main\java\com\tegufy\blackbox\utils\compat\LocaleQueryPolicy.java"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.LocaleQueryPolicyTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\javac.exe" --release 8 -Xlint:-options -encoding UTF-8 -d $out "$taskRoot\tests\ImportPolicyTest.java" "$taskRoot\app\src\main\java\app\containerlab\ImportPolicy.java" "$taskRoot\tests\HostPermissionPolicyTest.java" "$taskRoot\vendor\blackbox\Bcore\src\main\java\com\tegufy\blackbox\utils\compat\HostPermissionPolicy.java" "$taskRoot\tests\DiagnosticRedactorTest.java" "$taskRoot\app\src\main\java\app\containerlab\DiagnosticRedactor.java" "$taskRoot\tests\DiagnosticSummaryTest.java" "$taskRoot\app\src\main\java\app\containerlab\DiagnosticSummary.java" "$taskRoot\tests\ExitSummaryTest.java" "$taskRoot\app\src\main\java\app\containerlab\ExitSummary.java" "$taskRoot\tests\NativeCrashSummaryTest.java" "$taskRoot\app\src\main\java\app\containerlab\NativeCrashSummary.java"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\javac.exe" --release 8 -Xlint:-options -encoding UTF-8 -cp $out -d $out "$taskRoot\tests\CrashObserverTest.java" "$taskRoot\app\src\main\java\app\containerlab\CrashObserver.java" "$taskRoot\tests\RecentErrorSummaryTest.java" "$taskRoot\app\src\main\java\app\containerlab\RecentErrorSummary.java"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.CrashObserverTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.RecentErrorSummaryTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.ImportPolicyTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.HostPermissionPolicyTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.DiagnosticRedactorTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.DiagnosticSummaryTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.ExitSummaryTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& "$jdk\bin\java.exe" -cp $out app.containerlab.NativeCrashSummaryTest
exit $LASTEXITCODE
