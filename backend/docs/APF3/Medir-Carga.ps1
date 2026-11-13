param(
    [string]$BaseUrl='http://localhost:8080',
    [Parameter(Mandatory=$true)][string]$Token,
    [int]$Solicitudes=100,
    [string]$Ruta='/api/v1/reposicion?tamanio=20',
    [string]$Salida=(Join-Path $PSScriptRoot 'evidencias/carga.json')
)
$ErrorActionPreference='Stop'
if ($Solicitudes -lt 1) { throw 'Solicitudes debe ser positivo' }
$muestras=@(); $errores=0
for($n=0;$n -lt $Solicitudes;$n++){
    $reloj=[Diagnostics.Stopwatch]::StartNew()
    try { Invoke-RestMethod -Uri ($BaseUrl.TrimEnd('/')+$Ruta) -Headers @{Authorization="Bearer $Token"} | Out-Null }
    catch { $errores++ }
    finally { $reloj.Stop(); $muestras += $reloj.Elapsed.TotalMilliseconds }
}
$ordenadas=@($muestras | Sort-Object)
$resultado=[ordered]@{
    generadoEn=[DateTime]::UtcNow.ToString('o'); ruta=$Ruta; solicitudes=$Solicitudes
    concurrencia=1; errores=$errores; tasaErrores=($errores/$Solicitudes)
    promedioMs=($muestras | Measure-Object -Average).Average
    p95Ms=$ordenadas[[Math]::Ceiling($Solicitudes*0.95)-1]
    maximoMs=$ordenadas[-1]
    nota='Medicion secuencial del entorno indicado. No sustituye pruebas concurrentes ni un SLA acordado.'
}
New-Item -ItemType Directory -Force (Split-Path $Salida) | Out-Null
$resultado | ConvertTo-Json | Set-Content -LiteralPath $Salida -Encoding UTF8
$resultado
