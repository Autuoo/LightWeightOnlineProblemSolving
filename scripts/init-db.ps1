param(
    [Parameter(Mandatory = $true)][string]$Password,
    [string]$User = "root",
    [string]$HostName = "127.0.0.1",
    [int]$Port = 3306
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$schemaPath = Join-Path $repoRoot "docs\sql\schema.sql"

if (-not (Test-Path $schemaPath)) {
    throw "找不到 schema.sql: $schemaPath"
}

$schemaForMysql = $schemaPath.Replace("\", "/")
$env:MYSQL_PWD = $Password

try {
    & mysql --default-character-set=utf8mb4 -h $HostName -P $Port -u $User -e "source $schemaForMysql"
    if ($LASTEXITCODE -ne 0) {
        throw "数据库初始化失败，请检查账号密码与 MySQL 服务状态"
    }
    Write-Host "数据库初始化完成：oj 库与 6 张表已就绪"
} finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}