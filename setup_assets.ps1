<#
.SYNOPSIS
    下载并分发 KIGTTS 运行所需的模型资产（sosv-int8.zip / firefly.zip）。

.DESCRIPTION
    - sosv-int8.zip (ASR, ~209MB) 从 GitHub Release 下载
    - firefly.zip   (TTS, ~56MB)  从项目根目录的「流萤语音包.zip」或 Firefly 目录打包

    下载完成后自动复制到 android-app 和 flutter_app 的 assets 目录。

.NOTES
    用法:  powershell -ExecutionPolicy Bypass -File setup_assets.ps1
#>

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

# -- Target paths --
$androidAssets = Join-Path $root "android-app\app\src\main\assets"
$flutterAssets = Join-Path $root "flutter_app\android\app\src\main\assets"

foreach ($d in @($androidAssets, $flutterAssets)) {
    if (!(Test-Path $d)) { New-Item -ItemType Directory -Path $d -Force | Out-Null }
}

# -- 1. sosv-int8.zip --
$sosvUrl  = "https://github.com/HiMeditator/auto-caption/releases/download/sosv-model/sosv-int8.zip"
$sosvTmp  = Join-Path $root "sosv-int8.zip"

$sosvAndroid = Join-Path $androidAssets "sosv-int8.zip"
$sosvFlutter = Join-Path $flutterAssets "sosv-int8.zip"

$needDownload = $false
if (!(Test-Path $sosvAndroid)) { $needDownload = $true }
if (!(Test-Path $sosvFlutter)) { $needDownload = $true }

if ($needDownload) {
    if (Test-Path $sosvTmp) {
        Write-Host "sosv-int8: using existing $sosvTmp"
    } else {
        Write-Host "sosv-int8: downloading... (~209 MB)"
        Write-Host "  URL: $sosvUrl"
        try {
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $sosvUrl -OutFile $sosvTmp -UseBasicParsing
            $ProgressPreference = 'Continue'
        } catch {
            Write-Host ("sosv-int8: download failed: " + $_) -ForegroundColor Red
            Write-Host "  Please download manually:" -ForegroundColor Yellow
            Write-Host "  $sosvUrl" -ForegroundColor Cyan
            Write-Host "  Save to: $sosvTmp" -ForegroundColor Yellow
            $sosvTmp = $null
        }
    }
    if ($sosvTmp -and (Test-Path $sosvTmp)) {
        Copy-Item $sosvTmp $sosvAndroid -Force
        Write-Host ("sosv-int8 -> " + $sosvAndroid) -ForegroundColor Green
        Copy-Item $sosvTmp $sosvFlutter -Force
        Write-Host ("sosv-int8 -> " + $sosvFlutter) -ForegroundColor Green
    }
} else {
    Write-Host "sosv-int8: already exists, skipping." -ForegroundColor DarkGray
}

# -- 2. firefly.zip --
$fireflyAndroid = Join-Path $androidAssets "firefly.zip"
$fireflyFlutter = Join-Path $flutterAssets "firefly.zip"

# Source priority: root zip (Chinese name or firefly.zip) > Firefly dir > existing assets
$fireflySrc = $null
$fireflyDir = Join-Path $root "Firefly"

# Search for any voicepack zip in root (handles Chinese filename encoding issues)
$rootZips = Get-ChildItem -Path $root -Filter "*.zip" -File | Where-Object {
    $_.Name -match "firefly|voicepack" -or $_.Name -match "^(?!sosv).*\.zip$" -and $_.Length -gt 30000000 -and $_.Length -lt 100000000
}
# Also try the exact Chinese name via wildcard
$zhZip = Get-ChildItem -Path $root -Filter "*語音包*" -File -ErrorAction SilentlyContinue
if (!$zhZip) { $zhZip = Get-ChildItem -Path $root -Filter "*语音包*" -File -ErrorAction SilentlyContinue }
if ($zhZip) { $rootZips = @($zhZip) + @($rootZips) }

if ($rootZips -and $rootZips.Count -gt 0) {
    $fireflySrc = $rootZips[0].FullName
    Write-Host ("firefly: found source: " + $fireflySrc)
} elseif (Test-Path $fireflyDir) {
    # Pack from directory
    $tmpZip = Join-Path $root "firefly_tmp.zip"
    Write-Host "firefly: packing from Firefly directory..."
    Compress-Archive -Path (Join-Path $fireflyDir "*") -DestinationPath $tmpZip -Force
    $fireflySrc = $tmpZip
} elseif (Test-Path $fireflyAndroid) {
    $fireflySrc = $fireflyAndroid
} elseif (Test-Path $fireflyFlutter) {
    $fireflySrc = $fireflyFlutter
}

if ($fireflySrc) {
    if ($fireflySrc -ne $fireflyAndroid) {
        Copy-Item $fireflySrc $fireflyAndroid -Force
    }
    Write-Host ("firefly -> " + $fireflyAndroid) -ForegroundColor Green
    if ($fireflySrc -ne $fireflyFlutter) {
        Copy-Item $fireflySrc $fireflyFlutter -Force
    }
    Write-Host ("firefly -> " + $fireflyFlutter) -ForegroundColor Green
    # Clean up temp zip
    $tmpZip = Join-Path $root "firefly_tmp.zip"
    if (Test-Path $tmpZip) { Remove-Item $tmpZip -Force }
} else {
    Write-Host "firefly: not found. Place Firefly/ dir or voicepack zip in project root." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Green
Write-Host "You can now build android-app or flutter_app."
