param(
    [string]$Version = "3.12.4",
    [string]$Arch = "amd64",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BuildRoot = Join-Path $ProjectRoot "build"
$TargetDir = Join-Path $BuildRoot "python_bootstrap"
$CacheDir = Join-Path $BuildRoot "python-bootstrap-cache"
$MarkerPath = Join-Path $TargetDir ".bootstrap-version"
$ExpectedMarker = "python-$Version-embed-$Arch"

if ((-not $Force) -and (Test-Path (Join-Path $TargetDir "python.exe")) -and (Test-Path $MarkerPath)) {
    $marker = (Get-Content $MarkerPath -Raw).Trim()
    if ($marker -eq $ExpectedMarker) {
        Write-Host "== python bootstrap already prepared: $TargetDir =="
        exit 0
    }
}

New-Item -Path $BuildRoot -ItemType Directory -Force | Out-Null
New-Item -Path $CacheDir -ItemType Directory -Force | Out-Null

$zipName = "python-$Version-embed-$Arch.zip"
$zipPath = Join-Path $CacheDir $zipName
$url = "https://www.python.org/ftp/python/$Version/$zipName"

if ((-not (Test-Path $zipPath)) -or $Force) {
    Write-Host "== download python bootstrap: $url =="
    Invoke-WebRequest -Uri $url -OutFile $zipPath
}

if (Test-Path $TargetDir) {
    Remove-Item -LiteralPath $TargetDir -Recurse -Force
}
New-Item -Path $TargetDir -ItemType Directory -Force | Out-Null

Write-Host "== extract python bootstrap =="
Expand-Archive -LiteralPath $zipPath -DestinationPath $TargetDir -Force

$readme = @"
KIGTTS Trainer Python bootstrap

This is the official Windows embeddable Python runtime used only to start the
Electron Trainer backend, download runtime/resource archives, and invoke 7za.
Full training, ASR, Piper CUDA, and VoxCPM2 dependencies are intentionally not
bundled here; they are installed from external 7z runtime packages.
"@
Set-Content -LiteralPath (Join-Path $TargetDir "KIGTTS_BOOTSTRAP_README.txt") -Value $readme -Encoding UTF8
Set-Content -LiteralPath $MarkerPath -Value $ExpectedMarker -Encoding ASCII

$python = Join-Path $TargetDir "python.exe"
Write-Host "== validate python bootstrap =="
& $python -c "import encodings, json, pathlib, subprocess, threading, urllib.request, zipfile; print('bootstrap-ok')"
if ($LASTEXITCODE -ne 0) {
    throw "Python bootstrap validation failed"
}
