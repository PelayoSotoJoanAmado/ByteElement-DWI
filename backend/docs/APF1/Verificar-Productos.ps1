param(
    [string]$BaseUrl = 'http://localhost:8080',
    [long]$ProductoPersistido = 0
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
function Request([string]$Method, [string]$Path, [int]$Expected, [string]$Body = '') {
    $request = New-Object System.Net.Http.HttpRequestMessage ([System.Net.Http.HttpMethod]::new($Method)), ($BaseUrl + $Path)
    if ($Body) { $request.Content = New-Object System.Net.Http.StringContent $Body, ([Text.Encoding]::UTF8), 'application/json' }
    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ([int]$response.StatusCode -ne $Expected) { throw "$Method $Path esperaba $Expected; recibido $([int]$response.StatusCode): $content" }
    Write-Host "OK $Method $Path -> $Expected"
    $result = @{ Json = ($content | ConvertFrom-Json); Location = [string]$response.Headers.Location }
    $response.Dispose()
    $request.Dispose()
    return $result
}
try {
    if ($ProductoPersistido -gt 0) {
        $saved = Request GET "/api/v1/productos/$ProductoPersistido" 200
        if ($saved.Json.id -ne $ProductoPersistido) { throw 'ID persistido diferente' }
        Write-Host "Persistencia confirmada: $ProductoPersistido / $($saved.Json.sku)"
        return
    }
    $sku = 'QA-' + [guid]::NewGuid().ToString().ToUpperInvariant()
    $body = @{ sku = " $($sku.ToLowerInvariant()) "; categoria = 'Memorias RAM'; marca = 'Kingston'; modelo = 'DDR4 8 GB'; especificacion = 'DDR4, 8 GB, 3200 MHz'; critico = $true }
    $json = $body | ConvertTo-Json
    $created = Request POST '/api/v1/productos' 201 $json
    $id = $created.Json.id
    if ($created.Json.sku -cne $sku -or !$created.Json.activo -or !$created.Json.critico) { throw 'Normalizacion o estado inicial incorrecto' }
    if ($created.Location -ne "/api/v1/productos/$id") { throw 'Location incorrecto' }
    $read = Request GET "/api/v1/productos/$id" 200
    if ($read.Json.sku -cne $sku) { throw 'Producto consultado diferente' }
    $null = Request POST '/api/v1/productos' 409 $json
    $list = Request GET '/api/v1/productos?pagina=0&tamanio=1' 200
    if (@($list.Json).Count -gt 1) { throw 'Paginacion incorrecta' }
    $null = Request GET '/api/v1/productos/9223372036854775807' 404
    foreach ($query in @('pagina=-1','tamanio=0','tamanio=101','pagina=abc')) { $null = Request GET ('/api/v1/productos?' + $query) 400 }
    $body.sku = ' '
    $body.critico = $null
    $null = Request POST '/api/v1/productos' 400 ($body | ConvertTo-Json)
    $null = Request POST '/api/v1/productos' 400 '{'
    Write-Host "Producto de prueba conservado: $id / $sku"
    Write-Host "Tras reiniciar: .\docs\Verificar-Productos.ps1 -ProductoPersistido $id"
} finally { $client.Dispose() }
