param(
  [string]$OutputDir = "",
  [string]$PiperEnv = "",
  [string]$PiperCudaEnv = "",
  [string]$VoxCpmEnv = ""
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$trainerRoot = Resolve-Path (Join-Path $scriptDir "..")
$repoRoot = Resolve-Path (Join-Path $trainerRoot "..")

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
  $OutputDir = Join-Path $trainerRoot "dist-runtime"
}
if ([string]::IsNullOrWhiteSpace($PiperEnv)) {
  $PiperEnv = Join-Path $repoRoot "pc_trainer\piper_env"
}
if ([string]::IsNullOrWhiteSpace($PiperCudaEnv)) {
  $PiperCudaEnv = Join-Path $env:APPDATA "kgtts-trainer\runtimes\piper_env_cuda"
}
if ([string]::IsNullOrWhiteSpace($VoxCpmEnv)) {
  $VoxCpmEnv = Join-Path $env:APPDATA "kgtts-trainer\runtimes\voxcpm_env"
}

function Resolve-SevenZip {
  $candidates = @(
    (Join-Path $trainerRoot "node_modules\7zip-bin\win\x64\7za.exe"),
    (Join-Path $trainerRoot "build\7zip\7za.exe")
  )
  foreach ($candidate in $candidates) {
    if (Test-Path -LiteralPath $candidate) {
      return (Resolve-Path -LiteralPath $candidate).Path
    }
  }
  $external = Get-Command 7z -ErrorAction SilentlyContinue
  if ($external) {
    return $external.Source
  }
  $external = Get-Command 7za -ErrorAction SilentlyContinue
  if ($external) {
    return $external.Source
  }
  throw "7za.exe not found. Run npm install or add 7-Zip to PATH."
}

function Pack-Runtime {
  param(
    [string]$Name,
    [string]$SourceDir,
    [string]$ArchiveName,
    [string]$SevenZip,
    [string]$PackageType,
    [string]$EnvName,
    [string]$Remark
  )

  if (!(Test-Path -LiteralPath $SourceDir)) {
    Write-Warning "$Name source directory not found, skipped: $SourceDir"
    return
  }

  $resolvedOutputDir = (New-Item -ItemType Directory -Force -Path $OutputDir).FullName
  $archivePath = Join-Path $resolvedOutputDir $ArchiveName
  if (Test-Path -LiteralPath $archivePath) {
    Remove-Item -LiteralPath $archivePath -Force
  }

  $manifestPath = Join-Path $SourceDir "kigtts_runtime_manifest.json"
  $backupPath = ""
  if (Test-Path -LiteralPath $manifestPath) {
    $backupPath = Join-Path $env:TEMP ("kigtts_runtime_manifest_backup_" + [Guid]::NewGuid().ToString("N") + ".json")
    Move-Item -LiteralPath $manifestPath -Destination $backupPath -Force
  }
  $manifest = [ordered]@{
    schema_version = 1
    app = "KIGTTS Trainer"
    package_type = $PackageType
    env_name = $EnvName
    archive_name = $ArchiveName
    remark = $Remark
    created_at = (Get-Date).ToString("o")
  }

  Write-Host "Packing $Name -> $archivePath"
  try {
    $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
    Push-Location -LiteralPath $SourceDir
    & $SevenZip a -t7z -mx=9 $archivePath ".\*" | Write-Host
    if ($LASTEXITCODE -ne 0) {
      throw "$Name archive failed: $archivePath"
    }
  } finally {
    Pop-Location -ErrorAction SilentlyContinue
    if (Test-Path -LiteralPath $manifestPath) {
      Remove-Item -LiteralPath $manifestPath -Force
    }
    if ($backupPath -and (Test-Path -LiteralPath $backupPath)) {
      Move-Item -LiteralPath $backupPath -Destination $manifestPath -Force
    }
  }
}

$sevenZip = Resolve-SevenZip
Pack-Runtime -Name "Piper basic runtime" -SourceDir $PiperEnv -ArchiveName "piper_env.7z" -SevenZip $sevenZip -PackageType "piper_runtime" -EnvName "piper_env" -Remark "KIGTTS Trainer Piper CPU/basic runtime. Install only into the Piper basic runtime slot."
Pack-Runtime -Name "Piper CUDA runtime" -SourceDir $PiperCudaEnv -ArchiveName "piper_env_cuda.7z" -SevenZip $sevenZip -PackageType "piper_cuda_runtime" -EnvName "piper_env_cuda" -Remark "KIGTTS Trainer Piper CUDA runtime. Install only into the Piper CUDA runtime slot."
Pack-Runtime -Name "VoxCPM2 runtime" -SourceDir $VoxCpmEnv -ArchiveName "voxcpm_env.7z" -SevenZip $sevenZip -PackageType "voxcpm_runtime" -EnvName "voxcpm_env" -Remark "KIGTTS Trainer VoxCPM2 runtime. Install only into the VoxCPM2 runtime slot."

Write-Host "Runtime archives output: $OutputDir"
