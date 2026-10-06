$oldBase = "c:\Users\唐国几\IdeaProjects\yuegang-zhihui-ai1\ygh-platform\ygh-auth-service\src\main\java"
$newBase = "E:\yuegang-zhihui-ai\yuegang-zhihui-ai\ygh-platform\ygh-auth-service\src\main\java"

$oldFiles = Get-ChildItem -Path $oldBase -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$newFiles = Get-ChildItem -Path $newBase -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

Write-Host "=== Old files count: $($oldFiles.Count) ==="
Write-Host "=== New files count: $($newFiles.Count) ==="

$diffCount = 0
$sameCount = 0
$newOnlyCount = 0

foreach ($newFile in $newFiles) {
    $rel = $newFile.Substring($newBase.Length)
    $oldFile = $oldBase + $rel
    if (-not (Test-Path $oldFile)) {
        Write-Host "NEW ONLY: $rel"
        $newOnlyCount++
        continue
    }
    $newHash = (Get-FileHash $newFile -Algorithm MD5).Hash
    $oldHash = (Get-FileHash $oldFile -Algorithm MD5).Hash
    if ($newHash -ne $oldHash) {
        Write-Host "DIFF: $rel"
        $diffCount++
    } else {
        $sameCount++
    }
}

# Check old-only files
$oldOnlyCount = 0
foreach ($oldFile in $oldFiles) {
    $rel = $oldFile.Substring($oldBase.Length)
    $newFile = $newBase + $rel
    if (-not (Test-Path $newFile)) {
        Write-Host "OLD ONLY: $rel"
        $oldOnlyCount++
    }
}

Write-Host "`n=== Summary ==="
Write-Host "Same: $sameCount"
Write-Host "Different: $diffCount"
Write-Host "New only: $newOnlyCount"
Write-Host "Old only: $oldOnlyCount"
