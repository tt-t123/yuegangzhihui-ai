$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$services = @(
    @{Name="AUTH"; Port=8081},
    @{Name="USER"; Port=8082},
    @{Name="SYSTEM"; Port=8083},
    @{Name="GATEWAY"; Port=8080}
)

foreach ($svc in $services) {
    $url = "http://127.0.0.1:$($svc.Port)/actuator/health/liveness"
    Write-Host "=== $($svc.Name) ($($svc.Port)) liveness ===" -NoNewline
    try {
        $r = Invoke-WebRequest -Uri $url -Method GET -UseBasicParsing -TimeoutSec 5
        $j = $r.Content | ConvertFrom-Json
        Write-Host "  $($j.status)"
    } catch {
        Write-Host "  FAIL: $($_.Exception.Message)"
    }

    $url2 = "http://127.0.0.1:$($svc.Port)/actuator/health/readiness"
    Write-Host "=== $($svc.Name) ($($svc.Port)) readiness ===" -NoNewline
    try {
        $r = Invoke-WebRequest -Uri $url2 -Method GET -UseBasicParsing -TimeoutSec 5
        $j = $r.Content | ConvertFrom-Json
        Write-Host "  $($j.status)"
    } catch {
        Write-Host "  FAIL: $($_.Exception.Message)"
    }
}

Write-Host "`n=== Gateway captcha route test ==="
try {
    $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/v1/auth/captcha' -Method GET -UseBasicParsing -TimeoutSec 10
    $j = $r.Content | ConvertFrom-Json
    Write-Host "Captcha: $($j.code), challengeId present: $($null -ne $j.data.challengeId)"
} catch {
    Write-Host "FAIL: $($_.Exception.Message)"
}
