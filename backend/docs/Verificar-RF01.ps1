param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
function Request([string]$Method, [string]$Path, [int]$Expected, $Body = $null) {
    $request = New-Object System.Net.Http.HttpRequestMessage ([System.Net.Http.HttpMethod]::new($Method)), ($BaseUrl + $Path)
    if ($null -ne $Body) {
        $request.Content = New-Object System.Net.Http.StringContent ($Body | ConvertTo-Json), ([Text.Encoding]::UTF8), 'application/json'
    }
    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    try {
        if ([int]$response.StatusCode -ne $Expected) { throw "$Method $Path esperaba $Expected, recibido $([int]$response.StatusCode): $content" }
        Write-Host "OK $Method $Path -> $Expected"
        return @{ Json = ($content | ConvertFrom-Json); Location = [string]$response.Headers.Location }
    } finally { $response.Dispose(); $request.Dispose() }
}
try {
    $suffix = [guid]::NewGuid().ToString('N').Substring(0, 12).ToUpperInvariant()
    $producto = @{ sku = "RF01-$suffix"; categoria = 'RAM'; marca = 'Kingston'; modelo = 'DDR4'; especificacion = '8 GB'; critico = $true }
    $p = Request POST '/api/v1/productos' 201 $producto
    $productoId = $p.Json.id
    $producto.modelo = 'DDR4 actualizado'
    $p = Request PUT "/api/v1/productos/$productoId" 200 $producto
    if ($p.Json.modelo -ne 'DDR4 actualizado') { throw 'No se edito el modelo' }
    $null = Request POST '/api/v1/productos' 409 $producto

    $almacen = @{ codigo = "QA-$suffix"; nombre = 'Almacen RF01' }
    $a = Request POST '/api/v1/almacenes' 201 $almacen
    $almacenId = $a.Json.id
    if ($a.Location -ne "/api/v1/almacenes/$almacenId") { throw 'Location incorrecto' }
    $almacen.nombre = 'Almacen editado RF01'
    $a = Request PUT "/api/v1/almacenes/$almacenId" 200 $almacen
    if ($a.Json.nombre -ne $almacen.nombre) { throw 'No se edito el almacen' }
    $null = Request POST '/api/v1/almacenes' 409 $almacen
    $null = Request POST '/api/v1/almacenes' 400 @{ codigo = ' '; nombre = '' }

    $rutaPolitica = "/api/v1/productos/$productoId/almacenes/$almacenId/politica-stock"
    $null = Request GET $rutaPolitica 404
    $politica = Request PUT $rutaPolitica 200 @{ minimo = 0; maximo = 10 }
    $politicaId = $politica.Json.id
    $actualizada = Request PUT $rutaPolitica 200 @{ minimo = 2; maximo = 20 }
    if ($actualizada.Json.id -ne $politicaId) { throw 'PUT duplico la politica del par' }
    $null = Request PUT $rutaPolitica 400 @{ minimo = -1; maximo = 10 }
    $null = Request PUT $rutaPolitica 400 @{ minimo = 5; maximo = 5 }
    $null = Request PUT $rutaPolitica 400 @{ minimo = 5; maximo = 4 }
    $null = Request PUT $rutaPolitica 400 @{ minimo = 0 }
    $actualizada = Request GET $rutaPolitica 200
    if ($actualizada.Json.minimo -ne 2 -or $actualizada.Json.maximo -ne 20) { throw 'Un rechazo cambio los limites' }
    $null = Request PUT "/api/v1/productos/9223372036854775807/almacenes/$almacenId/politica-stock" 404 @{ minimo = 0; maximo = 1 }

    $entrada = @{ productoId = $productoId; almacenId = $almacenId; tipo = 'ENTRADA'; cantidad = 5; autor = 'Demostracion RF01'; motivo = 'Historial antes de desactivar' }
    $movimiento = Request POST '/api/v1/movimientos' 201 $entrada
    $null = Request PATCH "/api/v1/productos/$productoId/desactivar" 200
    $null = Request PATCH "/api/v1/productos/$productoId/desactivar" 200
    $p = Request GET "/api/v1/productos/$productoId" 200
    if ($p.Json.activo) { throw 'Producto sigue activo' }
    $null = Request POST '/api/v1/movimientos' 409 $entrada
    $null = Request PUT $rutaPolitica 409 @{ minimo = 0; maximo = 1 }
    $null = Request PATCH "/api/v1/almacenes/$almacenId/desactivar" 200
    $a = Request GET "/api/v1/almacenes/$almacenId" 200
    if ($a.Json.activo) { throw 'Almacen sigue activo' }
    $stock = Request GET "/api/v1/existencias?productoId=$productoId&almacenId=$almacenId" 200
    if ($stock.Json.cantidad -ne 5) { throw 'Desactivar modifico el saldo' }
    $null = Request GET ("/api/v1/movimientos/" + $movimiento.Json.id) 200
    $historial = Request GET "/api/v1/movimientos?productoId=$productoId&almacenId=$almacenId" 200
    if (@($historial.Json).Count -ne 1) { throw 'Historial incorrecto' }
    $null = Request GET $rutaPolitica 200
    $null = Request PATCH '/api/v1/productos/9223372036854775807/desactivar' 404
    $null = Request PUT '/api/v1/almacenes/9223372036854775807' 404 $almacen
    Write-Host "RF01 verificado. Datos QA conservados: producto=$productoId, almacen=$almacenId, politica=$politicaId"
} finally { $client.Dispose() }
