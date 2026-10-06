$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=== 1. AUTH captcha (direct) ==="
try {
    $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8081/api/v1/auth/captcha' -Method GET -UseBasicParsing -TimeoutSec 10
    Write-Host "Status: $($r.StatusCode)"
    $json = $r.Content | ConvertFrom-Json
    Write-Host "Code: $($json.code)"
    Write-Host "Has captchaId: $($null -ne $json.data.captchaId)"
    Write-Host "Has image: $($null -ne $json.data.image)"
    if ($json.data.captchaId) { Write-Host "captchaId: $($json.data.captchaId)" }
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            Write-Host "Body: $($reader.ReadToEnd())"
        } catch {}
    }
}

Write-Host "`n=== 2. Gateway route to AUTH captcha (via 8080) ==="
try {
    $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/v1/auth/captcha' -Method GET -UseBasicParsing -TimeoutSec 10
    Write-Host "Status: $($r.StatusCode)"
    $json = $r.Content | ConvertFrom-Json
    Write-Host "Code: $($json.code)"
    if ($json.data) {
        Write-Host "Has captchaId: $($null -ne $json.data.captchaId)"
        Write-Host "Has image: $($null -ne $json.data.image)"
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            Write-Host "Body: $($reader.ReadToEnd())"
        } catch {}
    }
}

Write-Host "`n=== 3. AUTH jwks (check JWT) ==="
try {
    $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8081/.well-known/jwks.json' -Method GET -UseBasicParsing -TimeoutSec 5
    Write-Host "Status: $($r.StatusCode)"
    Write-Host $r.Content.Substring(0, [Math]::Min(500, $r.Content.Length))
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}
