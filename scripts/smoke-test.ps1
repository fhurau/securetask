[CmdletBinding()]
param(
    [string]$KeycloakUrl = "http://localhost:8081",
    [string]$BackendApiUrl = "http://localhost:8080/api/v1",
    [string]$Realm = "securetask",
    [string]$SmokeClientId = "securetask-smoke-test",
    [string]$FrontendClientId = "securetask-frontend",
    [string]$OpenApiUrl = "",
    [string]$UploadStoragePath = "",
    [ValidateRange(1, 600)]
    [int]$StartupTimeoutSeconds = 90,
    [string]$User1Username = "user1@example.com",
    [string]$User1Password = "User123!",
    [string]$User2Username = "user2@example.com",
    [string]$User2Password = "User123!",
    [string]$AdminUsername = "admin@example.com",
    [string]$AdminPassword = "Admin123!",
    [string]$AuditorUsername = "auditor@example.com",
    [string]$AuditorPassword = "Auditor123!"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http

$script:Failed = $false
$script:ProjectId = $null
$script:User1Token = $null
$script:User2Token = $null
$script:AdminToken = $null
$script:AuditorToken = $null
$script:InitialUploadFiles = $null
$script:ResolvedUploadStoragePath = $null
$script:TemporaryFiles = @()

$KeycloakUrl = $KeycloakUrl.TrimEnd("/")
$BackendApiUrl = $BackendApiUrl.TrimEnd("/")
$tokenUrl = "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token"
$discoveryUrl = "$KeycloakUrl/realms/$Realm/.well-known/openid-configuration"

if ([string]::IsNullOrWhiteSpace($OpenApiUrl)) {
    $backendUri = [Uri]$BackendApiUrl
    $OpenApiUrl = "$($backendUri.Scheme)://$($backendUri.Authority)/v3/api-docs"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($UploadStoragePath)) {
    $UploadStoragePath = Join-Path $repoRoot "data\uploads"
}

$handler = New-Object System.Net.Http.HttpClientHandler
$handler.AllowAutoRedirect = $false
$http = New-Object System.Net.Http.HttpClient($handler)
$http.Timeout = [TimeSpan]::FromSeconds(20)

function Write-Pass {
    param([string]$Message)
    Write-Host "PASS  $Message" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Message)
    Write-Host "FAIL  $Message" -ForegroundColor Red
}

function Invoke-Http {
    param(
        [Parameter(Mandatory = $true)]
        [System.Net.Http.HttpMethod]$Method,
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [string]$Token,
        [System.Net.Http.HttpContent]$Content
    )

    $request = New-Object System.Net.Http.HttpRequestMessage($Method, $Url)
    try {
        if (-not [string]::IsNullOrWhiteSpace($Token)) {
            $request.Headers.Authorization =
                New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $Token)
        }
        if ($null -ne $Content) {
            $request.Content = $Content
        }

        $response = $http.SendAsync($request).GetAwaiter().GetResult()
        try {
            $bytes = $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
            return [pscustomobject]@{
                StatusCode = [int]$response.StatusCode
                Bytes = $bytes
                Text = [Text.Encoding]::UTF8.GetString($bytes)
            }
        }
        finally {
            $response.Dispose()
        }
    }
    finally {
        $request.Dispose()
    }
}

function Invoke-PreflightHttp {
    param(
        [Parameter(Mandatory = $true)]
        [System.Net.Http.HttpMethod]$Method,
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [Parameter(Mandatory = $true)]
        [int]$ExpectedStatus,
        [Parameter(Mandatory = $true)]
        [string]$Check,
        [Parameter(Mandatory = $true)]
        [string]$WaitMessage,
        [Parameter(Mandatory = $true)]
        [string]$Action,
        [scriptblock]$IsReady
    )

    $lastResult = "No response received."
    while ($true) {
        try {
            $response = Invoke-Http -Method $Method -Url $Url
            if ($response.StatusCode -eq $ExpectedStatus) {
                $ready = $true
                if ($null -ne $IsReady) {
                    try {
                        $ready = [bool](& $IsReady $response)
                    }
                    catch {
                        $ready = $false
                    }
                }
                if ($ready) {
                    return $response
                }
                $lastResult = "Received HTTP $($response.StatusCode), but the service did not report ready."
            }
            elseif ($response.StatusCode -ge 500 -and $response.StatusCode -le 599) {
                $lastResult = "Received temporary HTTP $($response.StatusCode)."
            }
            else {
                throw "$Check expected HTTP $ExpectedStatus but received HTTP $($response.StatusCode). $Action"
            }
        }
        catch {
            if ($_.Exception.Message -like "$Check expected HTTP*") {
                throw
            }
            $lastResult = "The HTTP request could not be completed."
        }

        $remaining = $script:StartupDeadline - [DateTimeOffset]::UtcNow
        if ($remaining.TotalSeconds -le 0) {
            throw "Startup readiness timed out after $StartupTimeoutSeconds seconds while waiting for $Check. $lastResult $Action"
        }

        Write-Host "WAIT  $WaitMessage" -ForegroundColor Yellow
        Start-Sleep -Milliseconds ([Math]::Min(2000, [Math]::Max(1, $remaining.TotalMilliseconds)))
    }
}

function New-JsonContent {
    param([Parameter(Mandatory = $true)]$Value)
    $json = $Value | ConvertTo-Json -Compress
    return New-Object System.Net.Http.StringContent(
        $json,
        [Text.Encoding]::UTF8,
        "application/json")
}

function Get-AccessToken {
    param(
        [Parameter(Mandatory = $true)][string]$ClientId,
        [Parameter(Mandatory = $true)][string]$Username,
        [Parameter(Mandatory = $true)][string]$Password
    )

    $fields = New-Object "System.Collections.Generic.Dictionary[string,string]"
    $fields.Add("grant_type", "password")
    $fields.Add("client_id", $ClientId)
    $fields.Add("username", $Username)
    $fields.Add("password", $Password)
    $content = New-Object System.Net.Http.FormUrlEncodedContent($fields)
    try {
        $response = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Post) `
            -Url $tokenUrl -Content $content
    }
    finally {
        $content.Dispose()
    }

    if ($response.StatusCode -ne 200) {
        throw "Token request for '$Username' using '$ClientId' returned HTTP $($response.StatusCode). Ensure the local realm contains the enabled smoke-test client and demo user."
    }

    $body = $response.Text | ConvertFrom-Json
    if ([string]::IsNullOrWhiteSpace($body.access_token)) {
        throw "Token response for '$Username' did not contain an access token."
    }
    return [string]$body.access_token
}

function Assert-Status {
    param(
        [Parameter(Mandatory = $true)]$Response,
        [Parameter(Mandatory = $true)][int]$Expected,
        [Parameter(Mandatory = $true)][string]$Check,
        [string]$Action
    )

    if ($Response.StatusCode -ne $Expected) {
        $message = "$Check expected HTTP $Expected but received HTTP $($Response.StatusCode)."
        if (-not [string]::IsNullOrWhiteSpace($Action)) {
            $message = "$message $Action"
        }
        throw $message
    }
    Write-Pass $Check
}

function Snapshot-UploadFiles {
    param([string]$Path)

    $resolved = [IO.Path]::GetFullPath($Path)
    $script:ResolvedUploadStoragePath = $resolved
    $files = New-Object "System.Collections.Generic.HashSet[string]" `
        ([StringComparer]::OrdinalIgnoreCase)

    if (Test-Path -LiteralPath $resolved) {
        Get-ChildItem -LiteralPath $resolved -File -Recurse | ForEach-Object {
            [void]$files.Add($_.FullName)
        }
    }
    return $files
}

function Remove-NewUploadFiles {
    if ($null -eq $script:InitialUploadFiles -or
            [string]::IsNullOrWhiteSpace($script:ResolvedUploadStoragePath) -or
            -not (Test-Path -LiteralPath $script:ResolvedUploadStoragePath)) {
        return
    }

    $storageRoot = [IO.Path]::GetFullPath($script:ResolvedUploadStoragePath)
    Get-ChildItem -LiteralPath $storageRoot -File -Recurse | ForEach-Object {
        $candidate = [IO.Path]::GetFullPath($_.FullName)
        if ($candidate.StartsWith($storageRoot, [StringComparison]::OrdinalIgnoreCase) -and
                -not $script:InitialUploadFiles.Contains($candidate)) {
            Remove-Item -LiteralPath $candidate -Force
        }
    }
}

try {
    Write-Host "distributed-auth-platform reviewer smoke test" -ForegroundColor Cyan
    Write-Host "Configuration: Keycloak=$KeycloakUrl Backend=$BackendApiUrl Realm=$Realm"
    Write-Host ""

    $script:InitialUploadFiles = Snapshot-UploadFiles -Path $UploadStoragePath

    $script:StartupDeadline =
        [DateTimeOffset]::UtcNow.AddSeconds($StartupTimeoutSeconds)

    $discovery = Invoke-PreflightHttp -Method ([System.Net.Http.HttpMethod]::Get) `
        -Url $discoveryUrl -ExpectedStatus 200 `
        -Check "Keycloak discovery endpoint" `
        -WaitMessage "Keycloak discovery endpoint is not ready yet, retrying..." `
        -Action "Check KeycloakUrl, Realm, and the running Docker Compose services."
    Assert-Status $discovery 200 "Keycloak discovery endpoint is reachable" `
        "Check KeycloakUrl, Realm, and the running Docker Compose services."

    $health = Invoke-PreflightHttp -Method ([System.Net.Http.HttpMethod]::Get) `
        -Url "$BackendApiUrl/health" -ExpectedStatus 200 `
        -Check "backend health endpoint" `
        -WaitMessage "Backend health endpoint is not ready yet, retrying..." `
        -Action "Check BackendApiUrl and backend container logs." `
        -IsReady {
            param($Response)
            return ($Response.Text | ConvertFrom-Json).status -eq "UP"
        }
    Assert-Status $health 200 "Backend health endpoint is reachable" `
        "Check BackendApiUrl and backend container logs."
    Write-Pass "Backend health reports UP"

    $openApi = Invoke-PreflightHttp -Method ([System.Net.Http.HttpMethod]::Get) `
        -Url $OpenApiUrl -ExpectedStatus 200 `
        -Check "OpenAPI endpoint" `
        -WaitMessage "OpenAPI endpoint is not ready yet, retrying..." `
        -Action "Set -OpenApiUrl when the backend uses a custom documentation path."
    Assert-Status $openApi 200 "OpenAPI document is reachable" `
        "Set -OpenApiUrl when the backend uses a custom documentation path."

    $anonymousProjects = Invoke-PreflightHttp `
        -Method ([System.Net.Http.HttpMethod]::Get) `
        -Url "$BackendApiUrl/projects" -ExpectedStatus 401 `
        -Check "unauthenticated projects check" `
        -WaitMessage "Unauthenticated projects check is not ready yet, retrying..." `
        -Action "The protected endpoint must not be public."
    Assert-Status $anonymousProjects 401 "Projects reject unauthenticated requests" `
        "The protected endpoint must not be public."

    $frontendFields = New-Object "System.Collections.Generic.Dictionary[string,string]"
    $frontendFields.Add("grant_type", "password")
    $frontendFields.Add("client_id", $FrontendClientId)
    $frontendFields.Add("username", $User1Username)
    $frontendFields.Add("password", $User1Password)
    $frontendContent = New-Object System.Net.Http.FormUrlEncodedContent($frontendFields)
    try {
        $frontendGrant = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Post) `
            -Url $tokenUrl -Content $frontendContent
    }
    finally {
        $frontendContent.Dispose()
    }
    if ($frontendGrant.StatusCode -eq 200) {
        throw "The frontend client accepted a password grant. Disable direct access grants on '$FrontendClientId'."
    }
    $frontendError = $null
    try {
        $frontendError = ($frontendGrant.Text | ConvertFrom-Json).error
    }
    catch {
        throw "Frontend password-grant rejection did not return a valid OAuth error. Confirm FrontendClientId and Keycloak configuration."
    }
    if ($frontendGrant.StatusCode -ne 400 -or $frontendError -ne "unauthorized_client") {
        throw "Frontend password-grant check returned HTTP $($frontendGrant.StatusCode) with OAuth error '$frontendError', not the expected unauthorized_client. Confirm '$FrontendClientId' exists and has direct access grants disabled."
    }
    Write-Pass "Frontend client rejects direct password grants"

    $script:User1Token = Get-AccessToken $SmokeClientId $User1Username $User1Password
    Write-Pass "Smoke client acquired a user1 token"
    $script:User2Token = Get-AccessToken $SmokeClientId $User2Username $User2Password
    Write-Pass "Smoke client acquired a user2 token"
    $script:AdminToken = Get-AccessToken $SmokeClientId $AdminUsername $AdminPassword
    Write-Pass "Smoke client acquired an admin token"
    $script:AuditorToken = Get-AccessToken $SmokeClientId $AuditorUsername $AuditorPassword
    Write-Pass "Smoke client acquired an auditor token"

    $projectContent = New-JsonContent @{
        name = "Smoke test $([Guid]::NewGuid().ToString('N').Substring(0, 8))"
        description = "Temporary reviewer smoke-test project"
    }
    try {
        $createProject = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Post) `
            -Url "$BackendApiUrl/projects" -Token $script:User1Token -Content $projectContent
    }
    finally {
        $projectContent.Dispose()
    }
    Assert-Status $createProject 201 "User1 can create a project"
    $script:ProjectId = [string](($createProject.Text | ConvertFrom-Json).id)
    if ([string]::IsNullOrWhiteSpace($script:ProjectId)) {
        throw "Project creation response did not include an ID."
    }

    $ownProject = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Get) `
        -Url "$BackendApiUrl/projects/$($script:ProjectId)" -Token $script:User1Token
    Assert-Status $ownProject 200 "User1 can read their own project"

    $otherProject = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Get) `
        -Url "$BackendApiUrl/projects/$($script:ProjectId)" -Token $script:User2Token
    Assert-Status $otherProject 403 "User2 cannot read user1's project" `
        "Object-level authorization may have been weakened."

    $txtPath = Join-Path ([IO.Path]::GetTempPath()) "securetask-smoke-$([Guid]::NewGuid()).txt"
    $exePath = Join-Path ([IO.Path]::GetTempPath()) "securetask-smoke-$([Guid]::NewGuid()).exe"
    $downloadPath = Join-Path ([IO.Path]::GetTempPath()) "securetask-smoke-$([Guid]::NewGuid())-download.txt"
    $script:TemporaryFiles = @($txtPath, $exePath, $downloadPath)
    $expectedText = "distributed-auth-platform smoke test $([Guid]::NewGuid())"
    [IO.File]::WriteAllText($txtPath, $expectedText, [Text.Encoding]::UTF8)
    [IO.File]::WriteAllText($exePath, "not an executable", [Text.Encoding]::UTF8)

    $txtMultipart = New-Object System.Net.Http.MultipartFormDataContent
    $txtFileBytes = [IO.File]::ReadAllBytes($txtPath)
    $txtBytes = New-Object System.Net.Http.ByteArrayContent `
        -ArgumentList (,$txtFileBytes)
    $txtBytes.Headers.ContentType =
        New-Object System.Net.Http.Headers.MediaTypeHeaderValue("text/plain")
    $txtMultipart.Add($txtBytes, "file", [IO.Path]::GetFileName($txtPath))
    try {
        $upload = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Post) `
            -Url "$BackendApiUrl/projects/$($script:ProjectId)/documents" `
            -Token $script:User1Token -Content $txtMultipart
    }
    finally {
        $txtMultipart.Dispose()
    }
    Assert-Status $upload 201 "User1 can upload an allowed TXT file"
    $documentId = [string](($upload.Text | ConvertFrom-Json).id)
    if ([string]::IsNullOrWhiteSpace($documentId)) {
        throw "Document upload response did not include an ID."
    }

    $download = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Get) `
        -Url "$BackendApiUrl/projects/$($script:ProjectId)/documents/$documentId" `
        -Token $script:User1Token
    Assert-Status $download 200 "User1 can download the uploaded document"
    [IO.File]::WriteAllBytes($downloadPath, $download.Bytes)
    if ([IO.File]::ReadAllText($downloadPath, [Text.Encoding]::UTF8) -ne $expectedText) {
        throw "Downloaded document content did not match the uploaded content."
    }
    Write-Pass "Downloaded document content matches"

    $exeMultipart = New-Object System.Net.Http.MultipartFormDataContent
    $exeFileBytes = [IO.File]::ReadAllBytes($exePath)
    $exeBytes = New-Object System.Net.Http.ByteArrayContent `
        -ArgumentList (,$exeFileBytes)
    $exeBytes.Headers.ContentType =
        New-Object System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream")
    $exeMultipart.Add($exeBytes, "file", [IO.Path]::GetFileName($exePath))
    try {
        $rejectedUpload = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Post) `
            -Url "$BackendApiUrl/projects/$($script:ProjectId)/documents" `
            -Token $script:User1Token -Content $exeMultipart
    }
    finally {
        $exeMultipart.Dispose()
    }
    Assert-Status $rejectedUpload 400 "Executable upload is rejected" `
        "Review the document extension allowlist."

    $adminAudit = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Get) `
        -Url "$BackendApiUrl/audit-logs" -Token $script:AdminToken
    Assert-Status $adminAudit 200 "Admin can read audit logs"

    $auditorAudit = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Get) `
        -Url "$BackendApiUrl/audit-logs" -Token $script:AuditorToken
    Assert-Status $auditorAudit 200 "Auditor can read audit logs"

    $userAudit = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Get) `
        -Url "$BackendApiUrl/audit-logs" -Token $script:User1Token
    Assert-Status $userAudit 403 "Normal user cannot read audit logs" `
        "Audit-log role authorization may have been weakened."

    Write-Host ""
    Write-Host "All smoke tests passed." -ForegroundColor Green
}
catch {
    $script:Failed = $true
    Write-Host ""
    Write-Fail $_.Exception.Message
}
finally {
    Write-Host ""
    Write-Host "Cleanup" -ForegroundColor Cyan

    if (-not [string]::IsNullOrWhiteSpace($script:ProjectId) -and
            -not [string]::IsNullOrWhiteSpace($script:User1Token)) {
        try {
            $cleanupProject = Invoke-Http -Method ([System.Net.Http.HttpMethod]::Delete) `
                -Url "$BackendApiUrl/projects/$($script:ProjectId)" `
                -Token $script:User1Token
            if ($cleanupProject.StatusCode -eq 204 -or $cleanupProject.StatusCode -eq 404) {
                Write-Pass "Temporary project metadata removed"
            }
            else {
                $script:Failed = $true
                Write-Fail "Temporary project cleanup returned HTTP $($cleanupProject.StatusCode). Delete project $($script:ProjectId) manually."
            }
        }
        catch {
            $script:Failed = $true
            Write-Fail "Temporary project cleanup failed. Delete project $($script:ProjectId) manually."
        }
    }

    try {
        Remove-NewUploadFiles
        Write-Pass "Smoke-test upload files removed"
    }
    catch {
        $script:Failed = $true
        Write-Fail "Upload cleanup failed under '$script:ResolvedUploadStoragePath'. Remove files created by this run manually."
    }

    foreach ($temporaryFile in $script:TemporaryFiles) {
        if (Test-Path -LiteralPath $temporaryFile) {
            Remove-Item -LiteralPath $temporaryFile -Force -ErrorAction SilentlyContinue
        }
    }

    $script:User1Token = $null
    $script:User2Token = $null
    $script:AdminToken = $null
    $script:AuditorToken = $null
    $http.Dispose()
    $handler.Dispose()
}

if ($script:Failed) {
    exit 1
}
exit 0
