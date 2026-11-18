param(
    [string]$BaseUrl = "http://127.0.0.1:8081",
    [string]$Username = "admin",
    [string]$Password = "Admin@123",
    [int]$ProblemId = 1,
    [string]$Language = "PYTHON3",
    [string]$Code = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Code)) {
    $Code = "a,b=map(int,input().split())`nprint(a+b)"
}

$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json -Compress
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.data.token)" }

$submitBody = @{ problemId = $ProblemId; language = $Language; code = $Code } | ConvertTo-Json -Compress
$submit = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/submissions" -ContentType "application/json" -Headers $headers -Body $submitBody
$sid = $submit.data.submissionId
Write-Host "[submit] id=$sid status=$($submit.data.status) reused=$($submit.data.reused)"

Start-Sleep -Seconds 6

$detail = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/submissions/$sid" -Headers $headers
Write-Host "[result] status=$($detail.data.status) score=$($detail.data.score) time=$($detail.data.execTimeMs)ms"
foreach ($item in $detail.data.cases) {
    Write-Host "    case#$($item.orderNo) $($item.status) $($item.execTimeMs)ms"
}