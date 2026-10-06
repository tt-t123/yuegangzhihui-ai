$ports = 8080, 8082, 8083, 8081
foreach ($p in $ports) {
    $l = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
    if ($l) { Write-Host "Port $p LISTENING" } else { Write-Host "Port $p NOT LISTENING" }
}
