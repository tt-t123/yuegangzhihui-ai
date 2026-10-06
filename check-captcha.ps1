$r = Invoke-WebRequest -Uri 'http://127.0.0.1:8081/api/v1/auth/captcha' -Method GET -UseBasicParsing
Write-Host "Status: $($r.StatusCode)"
Write-Host "Content (first 800 chars):"
Write-Host $r.Content.Substring(0, [Math]::Min(800, $r.Content.Length))
