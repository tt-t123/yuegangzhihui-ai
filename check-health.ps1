$ErrorActionPreference = "Continue"

Write-Host "=== AUTH 8081 ==="
try {
    $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8081/actuator/health' -Method GET -UseBasicParsing
    Write-Host "Status: $($r.StatusCode)"
    Write-Host $r.Content
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        Write-Host "Body: $body"
    }
}

Write-Host "`n=== Java Processes ==="
Get-Process -Name java -ErrorAction SilentlyContinue | ForEach-Object {
    $id = $_.Id
    $start = $_.StartTime
    $cmd = (Get-CimInstance Win32_Process -Filter "ProcessId=$id").CommandLine
    if ($cmd -match "spring-boot:run|ygh-") {
        Write-Host "PID: $id  Start: $start"
        Write-Host "CMD: $cmd"
        Write-Host "---"
    }
}
