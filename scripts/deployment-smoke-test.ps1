param(
    [string] $BaseUrl = "http://127.0.0.1:8088"
)

$ErrorActionPreference = "Stop"

function Assert-HttpOk {
    param([string] $Url)

    $response = Invoke-WebRequest -Uri $Url -UseBasicParsing
    if ($response.StatusCode -lt 200 -or $response.StatusCode -gt 299) {
        throw "Expected HTTP 2xx from $Url but received $($response.StatusCode)"
    }
}

Assert-HttpOk "$BaseUrl/"
Assert-HttpOk "$BaseUrl/actuator/health"

Write-Host "Deployment smoke test passed for $BaseUrl"
