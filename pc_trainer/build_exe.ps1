$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
if (-not $root) {
  $root = Split-Path -Parent $MyInvocation.MyCommand.Path
}
Set-Location $root

python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
pyinstaller --name kgt_trainer --noconsole -y `
  --add-data "data\phonemizer_zh.dict;data" `
  app.py

$distRoot = Join-Path $root "dist\kgt_trainer"
$resourcesRoot = Join-Path $root "resources_pack"

if (-not (Test-Path $resourcesRoot)) {
  Write-Warning "resources_pack not found, skipping resource copy."
} else {
  $subDirs = @("data", "Model", "piper_env", "tools", "logs")
  foreach ($d in $subDirs) {
    $src = Join-Path $resourcesRoot $d
    if (-not (Test-Path $src)) {
      Write-Warning "missing resource dir: $src"
      continue
    }

    $dst = Join-Path $distRoot $d
    New-Item -ItemType Directory -Force $dst | Out-Null

    robocopy $src $dst /MIR /NFL /NDL /NJH /NJS /NP | Out-Null
    $rc = $LASTEXITCODE
    if ($rc -ge 8) {
      throw "resource copy failed: $d (Robocopy exit code=$rc)"
    }
  }
}

Write-Host "Build complete. See dist\\kgt_trainer\\kgt_trainer.exe"
