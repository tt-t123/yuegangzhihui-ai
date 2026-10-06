$newBase = 'E:\yuegang-zhihui-ai\yuegang-zhihui-ai\ygh-web'
$oldBase = Join-Path (Get-Location) 'ygh-deploy\ygh-web'
$outFile = 'E:\frontend-compare.txt'
$script:results = @()
$script:results += "=== Frontend Comparison ==="
$script:results += "OldBase: $oldBase"
$script:results += "NewBase: $newBase"

function Compare-Dir {
    param($dir)
    $newDir = Join-Path $newBase $dir
    $oldDir = Join-Path $oldBase $dir
    $script:results += "=== $dir ==="
    $newExists = Test-Path $newDir
    $oldExists = Test-Path $oldDir
    if ($newExists -and $oldExists) {
        $newFiles = Get-ChildItem -Path $newDir -Recurse -File | ForEach-Object { $_.FullName.Substring($newDir.Length) }
        $oldFiles = Get-ChildItem -Path $oldDir -Recurse -File | ForEach-Object { $_.FullName.Substring($oldDir.Length) }
        $onlyNew = $newFiles | Where-Object { $oldFiles -notcontains $_ }
        $onlyOld = $oldFiles | Where-Object { $newFiles -notcontains $_ }
        if ($onlyNew) { $script:results += "  Only in NEW:"; foreach ($f in $onlyNew) { $script:results += "    $f" } }
        if ($onlyOld) { $script:results += "  Only in OLD:"; foreach ($f in $onlyOld) { $script:results += "    $f" } }
        $both = $newFiles | Where-Object { $oldFiles -contains $_ }
        $diffs = @()
        foreach ($f in $both) {
            $n = Join-Path $newDir $f
            $o = Join-Path $oldDir $f
            if ((Get-FileHash $n -Algorithm MD5).Hash -ne (Get-FileHash $o -Algorithm MD5).Hash) { $diffs += $f }
        }
        if ($diffs) { $script:results += "  DIFFERENT:"; foreach ($f in $diffs) { $script:results += "    $f" } }
    } else {
        $script:results += "  SKIP: new=$newExists old=$oldExists"
    }
}

Compare-Dir 'apps\ygh-web-mall\src'
Compare-Dir 'apps\ygh-web-admin\src'
Compare-Dir 'packages\ygh-web-shared\src'

$script:results += "=== End ==="
$script:results | Out-File $outFile -Encoding utf8
Write-Host "Done. Output saved to $outFile"
