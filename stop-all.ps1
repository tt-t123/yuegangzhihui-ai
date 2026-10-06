$javaProcs = Get-Process -Name java -ErrorAction SilentlyContinue
if ($javaProcs) {
    Write-Host "=== 当前Java进程 ==="
    foreach ($p in $javaProcs) {
        $cmd = (Get-CimInstance Win32_Process -Filter "ProcessId=$($p.Id)").CommandLine
        $short = if ($cmd -match "ygh-auth-service") { "AUTH" }
                 elseif ($cmd -match "ygh-user-service") { "USER" }
                 elseif ($cmd -match "ygh-system-service") { "SYSTEM" }
                 elseif ($cmd -match "ygh-gateway") { "GATEWAY" }
                 else { "OTHER" }
        Write-Host "  PID $($p.Id) -> $short"
    }
    Write-Host "`n=== 停止所有Java进程 ==="
    $javaProcs | Stop-Process -Force
    Write-Host "全部已停止"
} else {
    Write-Host "没有运行中的Java进程"
}
Start-Sleep -Seconds 2

Write-Host "`n=== 端口检查 ==="
$ports = 8080, 8081, 8082, 8083
foreach ($p in $ports) {
    $l = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
    if ($l) { Write-Host "  Port $p 仍被占用 PID $($l[0].OwningProcess)" }
    else { Write-Host "  Port $p 已释放" }
}
