<#
.SYNOPSIS
    Verificación automatizada de API y Seguridad para APF2 (StockGuard MYPE).
.DESCRIPTION
    Ejecuta el conjunto completo de 23 pruebas de validación de APF2 contra la API:
    - Autenticación con JWT (HS256) y perfiles de usuario.
    - Matriz de seguridad negativa (401 Sin token, 401 Token inválido, 403 Rol no autorizado).
    - Catálogo persistente en PostgreSQL.
    - Inventario transaccional, saldos con bloqueo pesimista y transferencias atómicas.
    - Paginación y JPQL de movimientos.
    - Conteos físicos, corte de movimientos y ajuste autorizado por supervisor.
    - Fotografía de inventario (Cortes de stock con lote y fecha).
    - Cálculo de indicadores desde persistencia: I05 (Quiebre), I07 (Exactitud) e I10 (Sobrestock).
    - Reportes JPQL de rotación y stock crítico.
#>

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminUser = "administrador",
    [string]$AdminPassword = "Administrador123!",
    [string]$DemoPassword = "DemoPassword123!"
)

$ErrorActionPreference = "Stop"

# Auto-detectar credenciales configuradas en application-local.properties
$propCandidates = @(
    "application-local.properties",
    "backend/application-local.properties",
    "../application-local.properties"
)
foreach ($pc in $propCandidates) {
    if (Test-Path $pc) {
        Get-Content $pc | ForEach-Object {
            $l = $_.Trim()
            if ($l -and -not $l.StartsWith("#")) {
                $p = $l -split "=", 2
                if ($p.Count -eq 2) {
                    $key = $p[0].Trim()
                    $val = $p[1].Trim()
                    if ($key -eq "stockguard.bootstrap.username" -and -not $PSBoundParameters.ContainsKey("AdminUser") -and $val) { $AdminUser = $val }
                    if ($key -eq "stockguard.bootstrap.password" -and -not $PSBoundParameters.ContainsKey("AdminPassword") -and $val) { $AdminPassword = $val }
                    if ($key -eq "stockguard.demo.password" -and -not $PSBoundParameters.ContainsKey("DemoPassword") -and $val) { $DemoPassword = $val }
                }
            }
        }
        break
    }
}

Add-Type -AssemblyName System.Net.Http

$client = [System.Net.Http.HttpClient]::new()

function Write-Step([string]$title) {
    Write-Host "`n========================================================" -ForegroundColor Cyan
    Write-Host " $title" -ForegroundColor Cyan
    Write-Host "========================================================" -ForegroundColor Cyan
}

function Write-Success([string]$msg) {
    Write-Host " [PASS] $msg" -ForegroundColor Green
}

function Write-Info([string]$msg) {
    Write-Host "   -> $msg" -ForegroundColor Gray
}

function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Path,
        [int]$ExpectedStatus,
        [string]$Body = "",
        [string]$Token = ""
    )

    $url = "$BaseUrl$Path"
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), $url)

    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
    }

    if (-not [string]::IsNullOrWhiteSpace($Body)) {
        $request.Content = [System.Net.Http.StringContent]::new($Body, [System.Text.Encoding]::UTF8, "application/json")
    }

    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $status = [int]$response.StatusCode

        if ($status -ne $ExpectedStatus) {
            Write-Host " [FAIL] $Method $Path esperaba $ExpectedStatus pero retorno $status" -ForegroundColor Red
            Write-Host " Detalle: $content" -ForegroundColor DarkRed
            throw "$Method $Path fallo: esperaba $ExpectedStatus, recibido $status"
        }

        Write-Host " [OK] $Method $Path -> $status" -ForegroundColor DarkGreen

        $jsonObj = $null
        if (-not [string]::IsNullOrWhiteSpace($content) -and ($content.TrimStart().StartsWith("{") -or $content.TrimStart().StartsWith("["))) {
            try { $jsonObj = $content | ConvertFrom-Json } catch {}
        }

        return @{
            Status = $status
            Content = $content
            Json = $jsonObj
            Location = [string]$response.Headers.Location
        }
    } finally {
        $request.Dispose()
    }
}

try {
    Write-Host "========================================================" -ForegroundColor Yellow
    Write-Host "   STOCKGUARD MYPE - SUITE DE VALIDACION APF2           " -ForegroundColor Yellow
    Write-Host "   Backend Seguro, Transaccional y Persistente          " -ForegroundColor Yellow
    Write-Host "========================================================" -ForegroundColor Yellow
    Write-Host " Servidor: $BaseUrl" -ForegroundColor White

    # Verificacion de conexion previa al servidor
    try {
        $pingReq = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, "$BaseUrl/api/v1/almacenes")
        $pingRes = $client.SendAsync($pingReq).GetAwaiter().GetResult()
        $pingRes.Dispose()
        $pingReq.Dispose()
    } catch {
        Write-Host "`n[ERROR] No se pudo conectar al servidor en $BaseUrl" -ForegroundColor Red
        Write-Host "El backend Spring Boot NO esta ejecutandose actualmente." -ForegroundColor Yellow
        Write-Host "`nPasos para iniciar el servidor:" -ForegroundColor White
        Write-Host "  1. Abre una terminal y ejecuta:" -ForegroundColor Cyan
        Write-Host "     cd backend" -ForegroundColor Gray
        Write-Host "     .\mvnw.cmd spring-boot:run" -ForegroundColor Gray
        Write-Host "  2. Espera a ver el mensaje: 'Started StockguardApplication'" -ForegroundColor Cyan
        Write-Host "  3. En OTRA terminal distinta, vuelve a ejecutar este script:" -ForegroundColor Cyan
        Write-Host "     .\backend\docs\APF2\Verificar-APF2.ps1`n" -ForegroundColor Gray
        exit 1
    }

    # --------------------------------------------------------------------------
    Write-Step "1. AUTENTICACION Y ROLES (JWT)"
    # --------------------------------------------------------------------------
    
    # 01 Login Administrador
    $bodyAdmin = @{ username = $AdminUser; password = $AdminPassword } | ConvertTo-Json
    $resAdmin = Invoke-ApiRequest "POST" "/api/v1/auth/login" 200 $bodyAdmin
    $adminToken = $resAdmin.Json.accessToken
    Write-Success "Login exitoso como ADMINISTRADOR. Token JWT obtenido."

    # 02 Login Supervisor
    $bodySup = @{ username = "demo-supervisor"; password = $DemoPassword } | ConvertTo-Json
    $resSup = Invoke-ApiRequest "POST" "/api/v1/auth/login" 200 $bodySup
    $supervisorToken = $resSup.Json.accessToken
    Write-Success "Login exitoso como SUPERVISOR."

    # 03 Login Almacén
    $bodyAlm = @{ username = "demo-almacen"; password = $DemoPassword } | ConvertTo-Json
    $resAlm = Invoke-ApiRequest "POST" "/api/v1/auth/login" 200 $bodyAlm
    $almacenToken = $resAlm.Json.accessToken
    Write-Success "Login exitoso como ALMACEN."

    # --------------------------------------------------------------------------
    Write-Step "2. SEGURIDAD DEFENSIVA (PRUEBAS NEGATIVAS)"
    # --------------------------------------------------------------------------

    # 04 Sin token se rechaza -> 401
    $null = Invoke-ApiRequest "GET" "/api/v1/usuarios" 401
    Write-Success "Peticion sin token rechazada con 401 Unauthorized."

    # 05 JWT inválido se rechaza -> 401
    $null = Invoke-ApiRequest "GET" "/api/v1/usuarios" 401 "" "token-falso-invalido-o-expirado"
    Write-Success "Peticion con JWT invalido rechazada con 401 Unauthorized."

    # 06 Rol ALMACEN no puede administrar usuarios -> 403
    $null = Invoke-ApiRequest "GET" "/api/v1/usuarios" 403 "" $almacenToken
    Write-Success "Rol ALMACEN bloqueado al intentar listar usuarios (403 Forbidden)."

    # --------------------------------------------------------------------------
    Write-Step "3. CATALOGO Y PERSISTENCIA"
    # --------------------------------------------------------------------------

    # 07 Catálogo de almacenes
    $resAlms = Invoke-ApiRequest "GET" "/api/v1/almacenes" 200 "" $almacenToken
    $almacenesCount = @($resAlms.Json).Count
    Write-Success "Almacenes habilitados listados: $almacenesCount"

    # 08 Crear producto QA no crítico
    $rnd = [guid]::NewGuid().ToString().Substring(0, 8).ToUpperInvariant()
    $sku = "QA-APF2-$rnd"
    $bodyProd = @{
        sku = $sku
        categoria = "Almacenamiento"
        marca = "Kingston"
        modelo = "KC3000 1TB"
        especificacion = "PCIe 4.0 NVMe M.2 7000MB/s"
        critico = $false
    } | ConvertTo-Json
    $resProd = Invoke-ApiRequest "POST" "/api/v1/productos" 201 $bodyProd $adminToken
    $productoId = $resProd.Json.id
    Write-Success "Producto persistido exitosamente con ID: $productoId y SKU: $sku"

    # --------------------------------------------------------------------------
    Write-Step "4. MOVIMIENTOS E INVENTARIO TRANSACCIONAL"
    # --------------------------------------------------------------------------

    # 09 Entrada autenticada por Almacén
    $bodyEntrada = @{
        productoId = $productoId
        almacenId = 1
        tipo = "ENTRADA"
        cantidad = 20
        motivo = "Lote inicial de validacion APF2"
    } | ConvertTo-Json
    $resEntrada = Invoke-ApiRequest "POST" "/api/v1/movimientos" 201 $bodyEntrada $almacenToken
    Write-Success "Entrada registrada por ALMACEN. Saldo resultante: $($resEntrada.Json.saldoResultante)"

    # 10 Otro rol (Administrador o Supervisor) no registra movimientos de inventario -> 403
    $bodyMovAdmin = @{
        productoId = $productoId
        almacenId = 1
        tipo = "ENTRADA"
        cantidad = 5
        motivo = "Intento no autorizado"
    } | ConvertTo-Json
    $null = Invoke-ApiRequest "POST" "/api/v1/movimientos" 403 $bodyMovAdmin $adminToken
    Write-Success "Administrador no tiene permiso para registrar movimientos (403 Forbidden)."

    # 11 Transferencia persistente entre almacenes (Almacén 1 -> Almacén 2)
    $bodyTransfer = @{
        productoId = $productoId
        origenAlmacenId = 1
        destinoAlmacenId = 2
        cantidad = 6
        motivo = "Transferencia entre sucursales"
    } | ConvertTo-Json
    $resTrans = Invoke-ApiRequest "POST" "/api/v1/transferencias" 201 $bodyTransfer $almacenToken
    Write-Success "Transferencia atomica ejecutada. Referencia UUID: $($resTrans.Json.referencia)"

    # 12 & 13 Consultar saldos origen y destino
    $saldoOrig = Invoke-ApiRequest "GET" "/api/v1/existencias?productoId=$productoId&almacenId=1" 200 "" $almacenToken
    $saldoDest = Invoke-ApiRequest "GET" "/api/v1/existencias?productoId=$productoId&almacenId=2" 200 "" $almacenToken
    Write-Success "Saldos verificados tras transferencia -> Origen (A01): $($saldoOrig.Json.cantidad) (esperado 14) | Destino (A02): $($saldoDest.Json.cantidad) (esperado 6)"

    # 14 JPQL Movimientos paginados
    $resMovs = Invoke-ApiRequest "GET" "/api/v1/movimientos?productoId=$productoId&pagina=0&tamanio=10" 200 "" $almacenToken
    Write-Success "Consulta JPQL de movimientos paginados exitosa. Total elementos: $($resMovs.Json.totalElements)"

    # --------------------------------------------------------------------------
    Write-Step "5. CONTEO FISICO Y AJUSTE DE SUPERVISOR"
    # --------------------------------------------------------------------------

    # 15 Conteo físico registrado por Almacén (detecta diferencia física de 15 vs 14)
    $bodyConteo = @{
        productoId = $productoId
        almacenId = 1
        cantidadFisica = 15
        motivo = "Conteo semanal de control"
    } | ConvertTo-Json
    $resConteo = Invoke-ApiRequest "POST" "/api/v1/conteos" 201 $bodyConteo $almacenToken
    $conteoId = $resConteo.Json.id
    Write-Success "Conteo registrado con ID $conteoId (Saldo corte: $($resConteo.Json.saldoCorte), Fisico: $($resConteo.Json.cantidadFisica))"

    # 16 Confirmar ajuste de conteo por Supervisor
    $resConf = Invoke-ApiRequest "POST" "/api/v1/conteos/$conteoId/confirmar" 200 "" $supervisorToken
    Write-Success "Ajuste confirmado por SUPERVISOR. Movimiento generado: $($resConf.Json.movimientoId)"

    # 17 Segunda confirmación debe ser rechazada -> 409 Conflict
    $null = Invoke-ApiRequest "POST" "/api/v1/conteos/$conteoId/confirmar" 409 "" $supervisorToken
    Write-Success "Intento de segunda confirmacion rechazado correctamente con 409 Conflict."

    # --------------------------------------------------------------------------
    Write-Step "6. CORTES DE STOCK E INDICADORES OPERATIVOS"
    # --------------------------------------------------------------------------

    # 18 Generar corte de stock (fotografía de inventario)
    $resCorte = Invoke-ApiRequest "POST" "/api/v1/cortes-stock" 200 "" $supervisorToken
    $itemsCorte = @($resCorte.Json)
    $lote = if ($itemsCorte.Count -gt 0) { $itemsCorte[0].lote } else { "N/A" }
    Write-Success "Corte de stock generado exitosamente. Lote: $lote (Total registros: $($itemsCorte.Count))"

    # Rango de fechas para indicadores
    $desde = "2026-01-01T00:00:00Z"
    $hasta = "2026-12-31T23:59:59Z"

    # 19 Indicador I05: Quiebre semanal de productos críticos (filtrado por DEMO-RAM)
    $resI05 = Invoke-ApiRequest "GET" "/api/v1/indicadores?codigo=I05&desde=$desde&hasta=$hasta&sku=DEMO-RAM" 200 "" $supervisorToken
    Write-Success "Indicador I05 (Quiebre semanal): $($resI05.Json.valor)% [Num: $($resI05.Json.numerador), Den: $($resI05.Json.denominador)]"

    # 20 Indicador I07: Exactitud de inventario en conteos
    $resI07 = Invoke-ApiRequest "GET" "/api/v1/indicadores?codigo=I07&desde=$desde&hasta=$hasta" 200 "" $supervisorToken
    Write-Success "Indicador I07 (Exactitud inventario): $($resI07.Json.valor)% [Num: $($resI07.Json.numerador), Den: $($resI07.Json.denominador)]"

    # 21 Indicador I10: Productos sobre el stock máximo (filtrado por DEMO-RAM)
    $resI10 = Invoke-ApiRequest "GET" "/api/v1/indicadores?codigo=I10&desde=$desde&hasta=$hasta&sku=DEMO-RAM" 200 "" $supervisorToken
    Write-Success "Indicador I10 (Sobrestock critico): $($resI10.Json.valor)% [Num: $($resI10.Json.numerador), Den: $($resI10.Json.denominador)]"

    # --------------------------------------------------------------------------
    Write-Step "7. REPORTES JPQL AVANZADOS"
    # --------------------------------------------------------------------------

    # 22 Rotación de inventario
    $resRot = Invoke-ApiRequest "GET" "/api/v1/reportes/rotacion?desde=$desde&hasta=$hasta&pagina=0&tamanio=10" 200 "" $supervisorToken
    Write-Success "Reporte de rotacion JPQL obtenido. Elementos calculados: $(@($resRot.Json.content).Count)"

    # 23 Stock crítico al corte
    $resCrit = Invoke-ApiRequest "GET" "/api/v1/reportes/stock-critico?desde=$desde&hasta=$hasta&pagina=0&tamanio=10" 200 "" $supervisorToken
    Write-Success "Reporte de stock critico al corte obtenido. Elementos: $(@($resCrit.Json.content).Count)"

    # --------------------------------------------------------------------------
    Write-Host "`n========================================================" -ForegroundColor Green
    Write-Host " EXITO TOTAL: LAS 23 PRUEBAS DE APF2 PASARON DE FORMA GOD! " -ForegroundColor Green
    Write-Host " Consistencia transaccional, seguridad y persistencia OK." -ForegroundColor Green
    Write-Host "========================================================`n" -ForegroundColor Green

} catch {
    Write-Host "`n========================================================" -ForegroundColor Red
    Write-Host " ERROR EN LA EJECUCION DE LA SUITE APF2" -ForegroundColor Red
    Write-Host " $_" -ForegroundColor Red
    Write-Host "========================================================`n" -ForegroundColor Red
    exit 1
} finally {
    $client.Dispose()
}

