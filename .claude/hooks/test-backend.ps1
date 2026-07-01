$json = [Console]::In.ReadToEnd()
try {
    $data = $json | ConvertFrom-Json
} catch {
    exit 0
}

$fp = $data.tool_input.file_path
if (-not $fp) { exit 0 }

if ($fp -match '[\\/]backend[\\/].*\.java$') {
    $backendDir = Join-Path $PSScriptRoot '..\..\backend'
    Push-Location $backendDir
    $output = & mvn -q test 2>&1
    $exitCode = $LASTEXITCODE
    Pop-Location
    if ($exitCode -eq 0) {
        Write-Output "PASS: backend tests passed after editing $fp"
    } else {
        Write-Output "FAIL: backend tests failed after editing $fp"
        Write-Output ($output -join "`n")
    }
}

exit 0
