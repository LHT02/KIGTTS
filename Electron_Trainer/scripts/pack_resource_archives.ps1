param(
  [string]$OutputDir = "",
  [string]$ResourcesPack = "",
  [string]$CkptDir = ""
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$trainerRoot = Resolve-Path (Join-Path $scriptDir "..")
$repoRoot = Resolve-Path (Join-Path $trainerRoot "..")

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
  $OutputDir = Join-Path $trainerRoot "dist-runtime"
}
if ([string]::IsNullOrWhiteSpace($ResourcesPack)) {
  $ResourcesPack = Join-Path $repoRoot "pc_trainer\resources_pack"
}
if ([string]::IsNullOrWhiteSpace($CkptDir)) {
  $CkptDir = Join-Path $repoRoot "pc_trainer\CKPT"
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

function Copy-DirectoryIfExists {
  param(
    [string]$Source,
    [string]$Destination
  )
  if (Test-Path -LiteralPath $Source) {
    $parent = Split-Path -Parent $Destination
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    Copy-Item -LiteralPath $Source -Destination $Destination -Recurse -Force
  }
}

function Copy-FileIfExists {
  param(
    [string]$Source,
    [string]$Destination
  )
  if (Test-Path -LiteralPath $Source) {
    $parent = Split-Path -Parent $Destination
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    Copy-Item -LiteralPath $Source -Destination $Destination -Force
  }
}

if (!(Test-Path -LiteralPath $ResourcesPack)) {
  throw "resources_pack not found: $ResourcesPack"
}

$resolvedOutputDir = (New-Item -ItemType Directory -Force -Path $OutputDir).FullName
$stagingRoot = Join-Path $resolvedOutputDir "_trainer_resources_staging"
$archivePath = Join-Path $resolvedOutputDir "trainer_resources.7z"

if (Test-Path -LiteralPath $stagingRoot) {
  Remove-Item -LiteralPath $stagingRoot -Recurse -Force
}
if (Test-Path -LiteralPath $archivePath) {
  Remove-Item -LiteralPath $archivePath -Force
}

New-Item -ItemType Directory -Force -Path $stagingRoot | Out-Null
$targetResources = Join-Path $stagingRoot "resources_pack"
$targetModel = Join-Path $targetResources "Model"

Copy-DirectoryIfExists -Source (Join-Path $ResourcesPack "data") -Destination (Join-Path $targetResources "data")
Copy-DirectoryIfExists -Source (Join-Path $ResourcesPack "tools") -Destination (Join-Path $targetResources "tools")
Copy-DirectoryIfExists -Source (Join-Path $ResourcesPack "logs") -Destination (Join-Path $targetResources "logs")
Copy-DirectoryIfExists -Source (Join-Path $ResourcesPack "Model\sosv-int8") -Destination (Join-Path $targetModel "sosv-int8")
Copy-DirectoryIfExists -Source (Join-Path $ResourcesPack "Model\piper_voices") -Destination (Join-Path $targetModel "piper_voices")
Copy-DirectoryIfExists -Source (Join-Path $ResourcesPack "Model\piper_checkpoints") -Destination (Join-Path $targetModel "piper_checkpoints")

$sourceModel = Join-Path $ResourcesPack "Model"
if (Test-Path -LiteralPath $sourceModel) {
  New-Item -ItemType Directory -Force -Path $targetModel | Out-Null
  Get-ChildItem -LiteralPath $sourceModel -File -Filter "*.zip" | ForEach-Object {
    Copy-FileIfExists -Source $_.FullName -Destination (Join-Path $targetModel $_.Name)
  }
}

if (Test-Path -LiteralPath $CkptDir) {
  $targetCkpt = Join-Path $stagingRoot "CKPT"
  New-Item -ItemType Directory -Force -Path $targetCkpt | Out-Null
  $resolvedCkptDir = (Resolve-Path -LiteralPath $CkptDir).Path
  Get-ChildItem -LiteralPath $CkptDir -File -Recurse -Filter "*.ckpt" | ForEach-Object {
    $relative = $_.FullName.Substring($resolvedCkptDir.Length).TrimStart([char[]]@('\', '/'))
    Copy-FileIfExists -Source $_.FullName -Destination (Join-Path $targetCkpt $relative)
  }
}

$manifest = [ordered]@{
  schema_version = 1
  app = "KIGTTS Trainer"
  package_type = "trainer_resources"
  resources_dir = "resources_pack"
  archive_name = "trainer_resources.7z"
  remark = "KIGTTS Trainer resource package. Includes ASR models, Piper baselines, phonemizer data and bundled helper tools. Install only into the trainer resources slot."
  created_at = (Get-Date).ToString("o")
}
$manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $stagingRoot "kigtts_resource_manifest.json") -Encoding UTF8

$sevenZip = Resolve-SevenZip
Write-Host "Packing trainer resources -> $archivePath"
try {
  Push-Location -LiteralPath $stagingRoot
  & $sevenZip a -t7z -mx=9 $archivePath ".\*" | Write-Host
  if ($LASTEXITCODE -ne 0) {
    throw "Trainer resources archive failed: $archivePath"
  }
} finally {
  Pop-Location -ErrorAction SilentlyContinue
  if (Test-Path -LiteralPath $stagingRoot) {
    Remove-Item -LiteralPath $stagingRoot -Recurse -Force
  }
}

Write-Host "Resource archive output: $archivePath"
