<#
.SYNOPSIS
    下载并分发 KIGTTS 运行所需的模型资产（sosv-int8.zip / firefly.zip）。

.DESCRIPTION
    - sosv-int8.zip (ASR, ~209MB) 从 GitHub Release 下载
    - firefly.zip   (TTS, ~56MB)  从项目根目录的「流萤语音包.zip」复制，
      或从 android-app assets 复制（如果根目录没有）

    下载完成后自动复制到 android-app 和 flutter_app 的 assets 目录。

.NOTES
    用法:  powershell -ExecutionPolicy Bypass -File setup_assets.ps1
#>

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

# ── 目标路径 ──────────────────────────────────────────────
$androidAssets = Join-Path $root "android-app\app\src\main\assets"
$flutterAssets = Join-Path $root "flutter_app\android\app\src\main\assets"

foreach ($d in @($androidAssets, $flutterAssets)) {
    if (!(Test-Path $d)) { New-Item -ItemType Directory -Path $d -Force | Out-Null }
}

# ── 1. sosv-int8.zip ─────────────────────────────────────
$sosvUrl  = "https://github.com/HiMeditator/auto-caption/releases/download/sosv-model/sosv-int8.zip"
$sosvTmp  = Join-Path $root "sosv-int8.zip"

$sosvTargets = @(
    (Join-Path $androidAssets "sosv-int8.zip"),
    (Join-Path $flutterAssets "sosv-int8.zip")
)

$needDownload = $false
foreach ($t in $sosvTargets) {
    if (!(Test-Path $t)) { $needDownload = $true; break }
}

if ($needDownload) {
    if (Test-Path $sosvTmp) {
        Write-Host "[sosv-int8] 使用已有的 $sosvTmp"
    } else {
        Write-Host "[sosv-int8] 下载中... (~209 MB，请耐心等待)"
        Write-Host "  URL: $sosvUrl"
        try {
            $ProgressPreference = 'SilentlyContinue'   # 关掉进度条，大幅加速
            Invoke-WebRequest -Uri $sosvUrl -OutFile $sosvTmp -UseBasicParsing
            $ProgressPreference = 'Continue'
        } catch {
            Write-Host "[sosv-int8] 自动下载失败: $_" -ForegroundColor Red
            Write-Host "  请手动下载:" -ForegroundColor Yellow
            Write-Host "  $sosvUrl" -ForegroundColor Cyan
            Write-Host "  保存到: $sosvTmp" -ForegroundColor Yellow
            Write-Host "  然后重新运行本脚本。"
            # 不退出，继续处理 firefly
            $sosvTmp = $null
        }
    }
    if ($sosvTmp -and (Test-Path $sosvTmp)) {
        foreach ($t in $sosvTargets) {
            Copy-Item $sosvTmp $t -Force
            Write-Host "[sosv-int8] -> $t" -ForegroundColor Green
        }
    }
} else {
    Write-Host "[sosv-int8] 已存在，跳过。" -ForegroundColor DarkGray
}

# ── 2. firefly.zip (流萤语音包) ──────────────────────────
$fireflyTargets = @(
    (Join-Path $androidAssets "firefly.zip"),
    (Join-Path $flutterAssets "firefly.zip")
)

# 来源优先级: 项目根目录的「流萤语音包.zip」 > android-app 已有的 > flutter 已有的
$fireflySrc = $null
$rootZh = Join-Path $root "流萤语音包.zip"
if (Test-Path $rootZh) {
    $fireflySrc = $rootZh
} elseif (Test-Path $fireflyTargets[0]) {
    $fireflySrc = $fireflyTargets[0]
} elseif (Test-Path $fireflyTargets[1]) {
    $fireflySrc = $fireflyTargets[1]
}

if ($fireflySrc) {
    foreach ($t in $fireflyTargets) {
        if ($fireflySrc -ne $t) {
            Copy-Item $fireflySrc $t -Force
        }
        Write-Host "[firefly] -> $t" -ForegroundColor Green
    }
} else {
    Write-Host "[firefly] 未找到流萤语音包。请将「流萤语音包.zip」放到项目根目录后重新运行。" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 完成 ===" -ForegroundColor Green
Write-Host "现在可以构建 android-app 或 flutter_app 了。"
