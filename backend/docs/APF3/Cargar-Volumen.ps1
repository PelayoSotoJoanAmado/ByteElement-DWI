param(
    [Parameter(Mandatory=$true)][string]$BasePruebas,
    [string]$PgBin='C:\Program Files\PostgreSQL\18\bin',
    [string]$HostPg='127.0.0.1',
    [int]$Puerto=5432,
    [Parameter(Mandatory=$true)][string]$Usuario
)
$ErrorActionPreference='Stop'
# PGPASSWORD o pgpass.conf suministran la credencial sin escribirla en el comando.
& (Join-Path $PgBin 'psql.exe') -h $HostPg -p $Puerto -U $Usuario -d $BasePruebas -v ON_ERROR_STOP=1 -f (Join-Path $PSScriptRoot 'datos/cargar-volumen.sql')
if ($LASTEXITCODE -ne 0) { throw 'La carga se revirtio. Revisar los errores de psql.' }
