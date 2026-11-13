# XSD BOOST build helper for Windows PowerShell 5.1+ / PowerShell 7+
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $Root
$GradleVersion = '9.3.1'
$ToolsDir = Join-Path $Root '.build-tools'
$GradleDir = Join-Path $ToolsDir "gradle-$GradleVersion"
$GradleExe = Join-Path $GradleDir 'bin/gradle.bat'
$DistDir = Join-Path $Root 'dist'

function Stop-Build([string]$Message) { Write-Host "`n[ERROR] $Message" -ForegroundColor Red; exit 1 }
Write-Host "`n[ XSD BOOST ] Memeriksa Java..." -ForegroundColor Cyan
$Java = Get-Command java -ErrorAction SilentlyContinue
if (-not $Java) { Stop-Build 'Java tidak ditemukan. Pasang JDK 17+ (disarankan JDK 21) dan atur PATH/JAVA_HOME.' }
$JavaLine = (& java -version 2>&1 | Select-Object -First 1).ToString()
if ($JavaLine -match '"(\d+)') { $JavaMajor = [int]$Matches[1] } else { Stop-Build "Tidak dapat membaca versi Java: $JavaLine" }
if ($JavaMajor -lt 17) { Stop-Build "Java $JavaMajor terdeteksi. Project membutuhkan JDK 17+." }
Write-Host "Java: $JavaLine"

$Sdk = $env:ANDROID_HOME
if (-not $Sdk) { $Sdk = $env:ANDROID_SDK_ROOT }
if (-not $Sdk) {
  $Candidates = @((Join-Path $env:LOCALAPPDATA 'Android/Sdk'), (Join-Path $HOME 'Android/Sdk'))
  foreach ($Candidate in $Candidates) { if (Test-Path (Join-Path $Candidate 'platforms')) { $Sdk = $Candidate; break } }
}
if (-not $Sdk -or -not (Test-Path $Sdk)) { Stop-Build 'Android SDK tidak ditemukan. Atur ANDROID_HOME ke folder SDK.' }
if (-not (Test-Path (Join-Path $Sdk 'platforms')) -or -not (Test-Path (Join-Path $Sdk 'build-tools'))) { Stop-Build 'SDK harus memiliki folder platforms dan build-tools.' }
$env:ANDROID_HOME = $Sdk; $env:ANDROID_SDK_ROOT = $Sdk
Write-Host "Android SDK: $Sdk"

$SystemGradle = Get-Command gradle -ErrorAction SilentlyContinue
if ($SystemGradle) { $GradleExe = $SystemGradle.Source; Write-Host "Menggunakan Gradle sistem: $GradleExe" }
elseif (-not (Test-Path $GradleExe)) {
  New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
  $Archive = Join-Path $ToolsDir "gradle-$GradleVersion-bin.zip"
  $Url = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"
  Write-Host "Mengunduh Gradle $GradleVersion..." -ForegroundColor Cyan
  try { Invoke-WebRequest -Uri $Url -OutFile $Archive }
  catch { Stop-Build "Gagal mengunduh Gradle: $($_.Exception.Message)" }
  Expand-Archive -LiteralPath $Archive -DestinationPath $ToolsDir -Force
}

Write-Host "`n1) Build APK Debug`n2) Build APK Release (perlu signing key)`n3) Clean project`n4) Build + Unit Test"
$Choice = Read-Host 'Pilih [1-4]'
switch ($Choice) {
  '1' { $Task = 'assembleDebug'; $Kind = 'debug' }
  '2' {
    if (-not $env:KEYSTORE_PATH -or -not $env:STORE_PASSWORD -or -not $env:KEY_PASSWORD) { Stop-Build 'Release perlu KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD. Lihat tools/README.md.' }
    if (-not (Test-Path $env:KEYSTORE_PATH)) { Stop-Build "Keystore tidak ditemukan: $env:KEYSTORE_PATH" }
    $Task = 'assembleRelease'; $Kind = 'release'
  }
  '3' { $Task = 'clean'; $Kind = 'clean' }
  '4' { $Task = 'testDebugUnitTest'; $Kind = 'test' }
  default { Stop-Build 'Pilihan tidak valid.' }
}
Write-Host "`nMenjalankan :app:$Task ..." -ForegroundColor Cyan
& $GradleExe --no-daemon --stacktrace ":app:$Task"
if ($LASTEXITCODE -ne 0) { Stop-Build "Gradle gagal dengan exit code $LASTEXITCODE. Lihat error di atas." }
if ($Kind -eq 'debug' -or $Kind -eq 'release') {
  $ApkName = if ($Kind -eq 'debug') { 'app-debug.apk' } else { 'app-release.apk' }
  $Apk = Join-Path $Root "app/build/outputs/apk/$Kind/$ApkName"
  if (-not (Test-Path $Apk)) { Stop-Build "APK tidak ditemukan: $Apk" }
  New-Item -ItemType Directory -Force -Path $DistDir | Out-Null
  Copy-Item $Apk (Join-Path $DistDir $ApkName) -Force
  Write-Host "`nBUILD BERHASIL: dist/$ApkName" -ForegroundColor Green
}
