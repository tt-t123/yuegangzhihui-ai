# Graceful shutdown for all ygh Spring Boot services.
# Strategy: send close signal (taskkill without /F) to trigger Spring Boot graceful shutdown,
# wait for GRACE_PERIOD, then force kill as fallback.
# Services configure server.shutdown=graceful + timeout-per-shutdown-phase=20s.

$GRACE_PERIOD = 25   # seconds to wait for graceful shutdown (covers 20s timeout-per-shutdown-phase)

function Get-YghAppProcesses {
    # Find all ygh Spring Boot application java processes (exclude mvn classworlds launcher)
    $procs = Get-CimInstance Win32_Process -Filter "Name='java.exe'"
    $apps = @()
    foreach ($p in $procs) {
        $cl = $p.CommandLine
        if ($cl -and $cl -match 'com\.yuegang\.zhihui\.[A-Za-z]+\.\w+Application') {
            $apps += [PSCustomObject]@{
                PID = $p.ProcessId
                App = ([regex]::Match($cl, 'com\.yuegang\.zhihui\.([A-Za-z]+)\.\w+Application')).Groups[1].Value
                Port = $null
            }
        }
    }
    foreach ($a in $apps) {
        $conn = Get-NetTCPConnection -OwningProcess $a.PID -State Listen -ErrorAction SilentlyContinue
        if ($conn) { $a.Port = ($conn.LocalPort -join ',') }
    }
    return $apps
}

function Stop-AppGraceful {
    param([int]$ProcessId, [string]$Name)
    $proc = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if (-not $proc) { Write-Host "  [skip] $Name (PID $ProcessId) not running"; return }
    Write-Host ("  [graceful] {0} (PID {1}) sending close signal..." -f $Name, $ProcessId)
    # taskkill without /F posts WM_CLOSE / close event so JVM shutdown hooks run (graceful shutdown).
    $null = & taskkill /PID $ProcessId 2>&1
    $deadline = (Get-Date).AddSeconds($GRACE_PERIOD)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 500
        if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
            Write-Host "  [done] $Name (PID $ProcessId) exited gracefully"
            return
        }
    }
    Write-Host ("  [fallback] {0} (PID {1}) graceful timeout, force kill" -f $Name, $ProcessId)
    & taskkill /F /PID $ProcessId 2>&1 | Out-Null
    Write-Host "  [done] $Name (PID $ProcessId) terminated"
}

Write-Host "=== Scan ygh Spring Boot processes ==="
$all = Get-YghAppProcesses
if ($all.Count -eq 0) { Write-Host "No ygh service processes running"; return }
$all | ForEach-Object { Write-Host ("  PID {0,-6} {1,-12} port={2}" -f $_.PID, $_.App, $_.Port) }

Write-Host "`n=== Step 1: stop gateway (8080) first ==="
# gateway 可能匹配到多个进程（mvn 启动器 + 应用），逐一关闭，仅保留监听 8080 的应用进程
$gw = $all | Where-Object { $_.App -eq 'gateway' -and $_.Port -notin @($null, '') }
if ($gw) {
    foreach ($g in $gw) {
        Stop-AppGraceful -ProcessId $g.PID -Name ("gateway:" + $g.Port)
    }
    $all = Get-YghAppProcesses
}

Write-Host "`n=== Step 2: stop remaining services ==="
foreach ($a in ($all | Sort-Object Port)) {
    Stop-AppGraceful -ProcessId $a.PID -Name $a.App
}

Start-Sleep -Seconds 2
Write-Host "`n=== Port check ==="
foreach ($port in @(8080,8081,8082,8083,8084,8085,8086,8087,8089,8090,8091,8092,8093,8094)) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($c) { Write-Host ("  Port {0} still occupied (PID {1})" -f $port, $c[0].OwningProcess) }
    else { Write-Host ("  Port {0} released" -f $port) }
}
Write-Host "`n=== All services shutdown complete ==="