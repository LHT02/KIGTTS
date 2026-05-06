param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

$pkg = Get-Content "$ProjectRoot\\package.json" -Raw | ConvertFrom-Json
$version = [string]$pkg.version
if ([string]::IsNullOrWhiteSpace($version)) {
    throw "Cannot read version from package.json"
}

$isccCandidates = @(
    "$env:LOCALAPPDATA\\Programs\\Inno Setup 6\\ISCC.exe",
    "$env:ProgramFiles(x86)\\Inno Setup 6\\ISCC.exe",
    "$env:ProgramFiles\\Inno Setup 6\\ISCC.exe"
)
$iscc = $isccCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $iscc) {
    $iscc = (Get-Command iscc.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -First 1)
}
if (-not $iscc) {
    throw "ISCC.exe not found. Please install Inno Setup 6."
}

if (-not $SkipBuild) {
    Write-Host "== prepare python bootstrap =="
    npm run prepare:bootstrap
    if ($LASTEXITCODE -ne 0) { throw "prepare python bootstrap failed" }

    Write-Host "== npm run build =="
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "npm run build failed" }

    Write-Host "== electron-builder --win --dir =="
    npx electron-builder --win --dir
    if ($LASTEXITCODE -ne 0) { throw "electron-builder --win --dir failed" }
}

$sourceDir = Resolve-Path "$ProjectRoot\\dist\\win-unpacked"
$bootstrapPython = Join-Path $sourceDir "resources\\python_bootstrap\\python.exe"
if (Test-Path $bootstrapPython) {
    Write-Host "== validate packaged python_bootstrap =="
    & $bootstrapPython -c "import encodings, json, pathlib, subprocess, sys, threading, urllib.request, zipfile; print(sys.executable)"
    if ($LASTEXITCODE -ne 0) {
        throw "Packaged python_bootstrap is incomplete or invalid"
    }
} else {
    throw "Packaged python_bootstrap not found"
}

Get-ChildItem (Join-Path $sourceDir "resources\\app.asar.unpacked\\backend") -Recurse -Force -Directory -Filter "__pycache__" -ErrorAction SilentlyContinue |
    ForEach-Object { Remove-Item -LiteralPath $_.FullName -Recurse -Force }

$outputDir = "$ProjectRoot\\dist\\inno"
if (-not (Test-Path $outputDir)) {
    New-Item -Path $outputDir -ItemType Directory | Out-Null
}

$iss = "$ProjectRoot\\build\\inno_installer.iss"
if (-not (Test-Path $iss)) {
    throw "Inno script not found: $iss"
}

Write-Host "== ISCC compile =="
& $iscc `
    "/DMyAppVersion=$version" `
    "/DSourceDir=$sourceDir" `
    "/DOutputDir=$outputDir" `
    $iss

if ($LASTEXITCODE -ne 0) {
    throw "ISCC compile failed"
}

Write-Host "== Inno artifacts =="
Get-ChildItem "$outputDir\\*.exe" -File |
    Sort-Object LastWriteTime -Descending |
    Select-Object Name, Length, LastWriteTime |
    Format-Table -AutoSize
