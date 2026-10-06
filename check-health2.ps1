$ErrorActionPreference = "Continue"

Write-Host "=== Ports Listening ==="
$ports = 8080,8081,8082,8083,8088
foreach ($p in $ports) {
    $listen = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
    if ($listen) {
        $proc = Get-Process -Id $listen[0].OwningProcess -ErrorAction SilentlyContinue
        Write-Host "Port $p : LISTENING (PID $($listen[0].OwningProcess), $($proc.ProcessName))"
    } else {
        Write-Host "Port $p : NOT LISTENING"
    }
}

Write-Host "`n=== AUTH health components (try several endpoints) ==="
$endpoints = @(
    'http://127.0.0.1:8081/actuator/health/liveness',
    'http://127.0.0.1:8081/actuator/health/readiness',
    'http://127.0.0.1:8081/actuator/health',
    'http://127.0.0.1:8081/livez',
    'http://127.0.0.1:8081/readyz'
)
foreach ($url in $endpoints) {
    Write-Host "`n--- GET $url ---"
    try {
        $r = Invoke-WebRequest -Uri $url -Method GET -UseBasicParsing
        Write-Host "Status: $($r.StatusCode)"
        Write-Host $r.Content
    } catch {
        Write-Host "Error: $($_.Exception.Message)"
        if ($_.Exception.Response) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $body = $reader.ReadToEnd()
                Write-Host "Body: $body"
            } catch {}
        }
    }
}

Write-Host "`n=== Test gateway port 8080 ==="
try {
    $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/actuator/health' -Method GET -UseBasicParsing -TimeoutSec 5
    Write-Host "Status: $($r.StatusCode)"
    Write-Host $r.Content
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}
