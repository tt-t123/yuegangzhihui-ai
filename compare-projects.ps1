$newBase = 'E:\yuegang-zhihui-ai\yuegang-zhihui-ai'
$oldBase = Get-Location
$outFile = 'E:\compare-result.txt'
$script:results = @()
$script:results += "=== Comparison Start ==="
$script:results += "OldBase: $oldBase"
$script:results += "NewBase: $newBase"

function Compare-Dir {
    param($dir, $filter)
    $newDir = Join-Path $newBase $dir
    $oldDir = Join-Path $oldBase $dir
    $script:results += "=== $dir ==="
    $newExists = Test-Path $newDir
    $oldExists = Test-Path $oldDir
    if ($newExists -and $oldExists) {
        if ($filter -eq 'java') {
            $newFiles = Get-ChildItem -Path $newDir -Recurse -Filter *.java | ForEach-Object { $_.FullName.Substring($newDir.Length) }
            $oldFiles = Get-ChildItem -Path $oldDir -Recurse -Filter *.java | ForEach-Object { $_.FullName.Substring($oldDir.Length) }
        } else {
            $newFiles = Get-ChildItem -Path $newDir -Recurse -File | Where-Object { $_.Extension -ne '.class' } | ForEach-Object { $_.FullName.Substring($newDir.Length) }
            $oldFiles = Get-ChildItem -Path $oldDir -Recurse -File | Where-Object { $_.Extension -ne '.class' } | ForEach-Object { $_.FullName.Substring($oldDir.Length) }
        }
        $onlyNew = $newFiles | Where-Object { $oldFiles -notcontains $_ }
        $onlyOld = $oldFiles | Where-Object { $newFiles -notcontains $_ }
        if ($onlyNew) { $script:results += "  Only in NEW:"; foreach ($f in $onlyNew) { $script:results += "    $f" } }
        if ($onlyOld) { $script:results += "  Only in OLD:"; foreach ($f in $onlyOld) { $script:results += "    $f" } }
        $both = $newFiles | Where-Object { $oldFiles -contains $_ }
        $diffs = @()
        foreach ($f in $both) {
            $n = Join-Path $newDir $f
            $o = Join-Path $oldDir $f
            $nh = (Get-FileHash $n -Algorithm MD5).Hash
            $oh = (Get-FileHash $o -Algorithm MD5).Hash
            if ($nh -ne $oh) { $diffs += $f }
        }
        if ($diffs) { $script:results += "  DIFFERENT:"; foreach ($f in $diffs) { $script:results += "    $f" } }
    } else {
        $script:results += "  SKIP: new=$newExists old=$oldExists"
        $script:results += "    newDir=$newDir"
        $script:results += "    oldDir=$oldDir"
    }
}

Compare-Dir 'ygh-platform\ygh-auth-service\src\main\java' 'java'
Compare-Dir 'ygh-applications\ygh-user\ygh-user-service\src\main\java' 'java'
Compare-Dir 'ygh-applications\ygh-system\ygh-system-service\src\main\java' 'java'
Compare-Dir 'ygh-platform\ygh-gateway\src\main\java' 'java'
Compare-Dir 'ygh-common\ygh-common-core\src\main\java' 'java'
Compare-Dir 'ygh-common\ygh-common-web\src\main\java' 'java'
Compare-Dir 'ygh-common\ygh-common-redis\src\main\java' 'java'
Compare-Dir 'ygh-common\ygh-common-security\src\main\java' 'java'
Compare-Dir 'ygh-common\ygh-common-mybatis\src\main\java' 'java'
Compare-Dir 'ygh-common\ygh-common-mq\src\main\java' 'java'
Compare-Dir 'ygh-platform\ygh-auth-service\src\main\resources' 'all'
Compare-Dir 'ygh-applications\ygh-user\ygh-user-service\src\main\resources' 'all'
Compare-Dir 'ygh-applications\ygh-system\ygh-system-service\src\main\resources' 'all'
Compare-Dir 'ygh-platform\ygh-gateway\src\main\resources' 'all'

$script:results += "=== Comparison End ==="
$script:results | Out-File $outFile -Encoding utf8
Write-Host "Done. Output saved to $outFile"
