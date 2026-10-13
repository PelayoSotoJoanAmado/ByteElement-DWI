param(
    [string]$BaseUrl = 'http://localhost:8080'
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
    Write-Host "--- 1. Verificar catálogo de almacenes ---"
    $almacenes = Request GET '/api/v1/almacenes' 200
    if (@($almacenes.Json).Count -lt 3) { throw 'Se esperaban 3 almacenes (A01, A02, A03)' }
    Write-Host "Almacenes listados correctamente: $(@($almacenes.Json).Count)"

    Write-Host "`n--- 2. Crear producto para prueba de inventario ---"
    $sku = 'QA-INV-' + [guid]::NewGuid().ToString().Substring(0, 8).ToUpperInvariant()
    $bodyProducto = @{
        sku = $sku
        categoria = 'Almacenamiento'
        marca = 'Kingston'
        modelo = 'NV2 1TB'
        especificacion = 'PCIe 4.0 NVMe M.2 SSD'
        critico = $true
    }
    $productoCreado = Request POST '/api/v1/productos' 201 ($bodyProducto | ConvertTo-Json)
    $productoId = $productoCreado.Json.id
    Write-Host "Producto creado con ID: $productoId y SKU: $sku"

    Write-Host "`n--- 3. Consultar existencia inicial (debe ser 0) ---"
    $existenciaIni = Request GET "/api/v1/existencias?productoId=$productoId&almacenId=1" 200
    if ($existenciaIni.Json.cantidad -ne 0) { throw 'El stock inicial debio ser 0' }

    Write-Host "`n--- 4. Registrar movimiento de ENTRADA (10 unidades) ---"
    $movEntrada = @{
        productoId = $productoId
        almacenId = 1
        tipo = 'ENTRADA'
        cantidad = 10
        autor = 'Operador APF1'
        motivo = 'Recepcion de lote inicial'
    }
    $resEntrada = Request POST '/api/v1/movimientos' 201 ($movEntrada | ConvertTo-Json)
    $movEntradaId = $resEntrada.Json.id
    if ($resEntrada.Json.saldoResultante -ne 10) { throw 'El saldo resultante debio ser 10' }
    if ($resEntrada.Location -ne "/api/v1/movimientos/$movEntradaId") { throw 'Location de movimiento incorrecto' }

    Write-Host "`n--- 5. Verificar stock tras entrada ---"
    $existenciaPostEntrada = Request GET "/api/v1/existencias?productoId=$productoId&almacenId=1" 200
    if ($existenciaPostEntrada.Json.cantidad -ne 10) { throw 'El stock debio ser 10' }

    Write-Host "`n--- 6. Registrar movimiento de SALIDA valida (4 unidades) ---"
    $movSalida = @{
        productoId = $productoId
        almacenId = 1
        tipo = 'SALIDA'
        cantidad = 4
        autor = 'Operador APF1'
        motivo = 'Despacho comercial'
    }
    $resSalida = Request POST '/api/v1/movimientos' 201 ($movSalida | ConvertTo-Json)
    if ($resSalida.Json.saldoResultante -ne 6) { throw 'El saldo resultante debio ser 6' }

    Write-Host "`n--- 7. Verificar stock tras salida valida ---"
    $existenciaPostSalida = Request GET "/api/v1/existencias?productoId=$productoId&almacenId=1" 200
    if ($existenciaPostSalida.Json.cantidad -ne 6) { throw 'El stock debio ser 6' }

    Write-Host "`n--- 8. Probar regla de negocio: Rechazar SALIDA excesiva (15 unidades teniendo 6) -> 409 Conflict ---"
    $movSalidaExcesiva = @{
        productoId = $productoId
        almacenId = 1
        tipo = 'SALIDA'
        cantidad = 15
        autor = 'Operador APF1'
        motivo = 'Intento de venta sin stock'
    }
    $null = Request POST '/api/v1/movimientos' 409 ($movSalidaExcesiva | ConvertTo-Json)

    Write-Host "`n--- 9. Comprobar que el saldo se conservo intacto en 6 tras el rechazo ---"
    $existenciaConservada = Request GET "/api/v1/existencias?productoId=$productoId&almacenId=1" 200
    if ($existenciaConservada.Json.cantidad -ne 6) { throw 'El saldo fue modificado indebidamente tras la salida rechazada' }

    Write-Host "`n--- 10. Consultar detalle de un movimiento por ID ---"
    $movConsultado = Request GET "/api/v1/movimientos/$movEntradaId" 200
    if ($movConsultado.Json.id -ne $movEntradaId -or $movConsultado.Json.cantidad -ne 10) { throw 'Datos de movimiento consultado incorrectos' }

    Write-Host "`n--- 11. Consultar un almacén individual por ID ---"
    $alm1 = Request GET '/api/v1/almacenes/1' 200
    if ($alm1.Json.codigo -ne 'A01') { throw 'Codigo de almacen 1 incorrecto' }

    Write-Host "`n--- 12. Regla Crítica: Transferencia atómica entre almacenes (A01 -> A02, 3 unidades) ---"
    $transReq = @{
        productoId = $productoId
        origenAlmacenId = 1
        destinoAlmacenId = 2
        cantidad = 3
        autor = 'Supervisor APF1'
        motivo = 'Reabastecimiento interno de tienda a deposito'
    }
    $resTrans = Request POST '/api/v1/transferencias' 201 ($transReq | ConvertTo-Json)
    if ($resTrans.Json.saldoOrigenResultante -ne 3 -or $resTrans.Json.saldoDestinoResultante -ne 3) {
        throw 'Saldos resultantes de transferencia atomica incorrectos'
    }

    Write-Host "`n--- 13. Comprobar saldos en ambos almacenes tras la transferencia ---"
    $stockOrigen = Request GET "/api/v1/existencias?productoId=$productoId&almacenId=1" 200
    $stockDestino = Request GET "/api/v1/existencias?productoId=$productoId&almacenId=2" 200
    if ($stockOrigen.Json.cantidad -ne 3) { throw 'El stock de origen deberia ser 3' }
    if ($stockDestino.Json.cantidad -ne 3) { throw 'El stock de destino deberia ser 3' }
    Write-Host "Transferencia confirmada: Origen=$($stockOrigen.Json.cantidad), Destino=$($stockDestino.Json.cantidad)"

    Write-Host "`n--- 14. Regla Crítica: Rechazar transferencia con saldo insuficiente en origen (409 Conflict) ---"
    $transExcesiva = @{
        productoId = $productoId
        origenAlmacenId = 1
        destinoAlmacenId = 2
        cantidad = 20
        autor = 'Supervisor APF1'
        motivo = 'Intento de transferir mas de lo disponible'
    }
    $null = Request POST '/api/v1/transferencias' 409 ($transExcesiva | ConvertTo-Json)

    Write-Host "`n--- 15. Regla de Integridad: Rechazar transferencia al mismo almacén (400 Bad Request) ---"
    $transMismoAlm = @{
        productoId = $productoId
        origenAlmacenId = 1
        destinoAlmacenId = 1
        cantidad = 2
        autor = 'Supervisor APF1'
        motivo = 'Origen y destino iguales'
    }
    $null = Request POST '/api/v1/transferencias' 400 ($transMismoAlm | ConvertTo-Json)

    Write-Host "`n--- 16. Listar auditoría e historial de movimientos ---"
    $movs = Request GET "/api/v1/movimientos?productoId=$productoId" 200
    if (@($movs.Json).Count -ne 4) { throw 'Deben existir exactamente 4 movimientos; los rechazos no deben generar historial' }
    Write-Host "Historial trazable confirmado: $(@($movs.Json).Count) movimientos para el producto $productoId"

    Write-Host "`n--- 17. Validar rechazo de datos inválidos (400 Bad Request) ---"
    $movInvalido = @{
        productoId = -1
        almacenId = $null
        tipo = 'SALIDA'
        cantidad = 0
        autor = ''
        motivo = ''
    }
    $null = Request POST '/api/v1/movimientos' 400 ($movInvalido | ConvertTo-Json)

    $null = Request GET '/api/v1/existencias?productoId=-1&almacenId=1' 400
    $null = Request GET '/api/v1/existencias?productoId=9223372036854775807&almacenId=1' 404
    $origenFinal = Request GET "/api/v1/existencias?productoId=$productoId&almacenId=1" 200
    $destinoFinal = Request GET "/api/v1/existencias?productoId=$productoId&almacenId=2" 200
    if ($origenFinal.Json.cantidad -ne 3 -or $destinoFinal.Json.cantidad -ne 3) {
        throw 'Una transferencia rechazada modifico los saldos'
    }

    Write-Host "`n========================================================"
    Write-Host " TODAS LAS PRUEBAS DE LA API APF1 PASARON EXITOSAMENTE! "
    Write-Host "========================================================"
} finally {
    $client.Dispose()
}
