$json = [Console]::In.ReadToEnd()
try {
    $data = $json | ConvertFrom-Json
} catch {
    exit 0
}

$fp = $data.tool_input.file_path
if (-not $fp) { exit 0 }

if ($fp -notmatch '[\\/]backend[\\/].*\.java$') { exit 0 }

$backendDir = Join-Path $PSScriptRoot '..\..\backend'
$pomText = Get-Content (Join-Path $backendDir 'pom.xml') -Raw

Push-Location $backendDir
if ($pomText -match 'spotless-maven-plugin') {
    & mvn -q spotless:apply
    Write-Output "spotless:apply ran on $fp"
} elseif ($pomText -match 'maven-checkstyle-plugin') {
    & mvn -q checkstyle:check
    Write-Output "checkstyle:check ran on $fp"
}
Pop-Location

exit 0
