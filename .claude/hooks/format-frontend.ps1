$json = [Console]::In.ReadToEnd()
try {
    $data = $json | ConvertFrom-Json
} catch {
    exit 0
}

$fp = $data.tool_input.file_path
if (-not $fp) { exit 0 }

if ($fp -match '[\\/]frontend[\\/].*\.(ts|tsx|js|jsx)$' -and (Test-Path $fp)) {
    $frontendDir = Join-Path $PSScriptRoot '..\..\frontend'
    Push-Location $frontendDir
    npx eslint --fix "$fp"
    Pop-Location
}

exit 0
