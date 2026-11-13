param(
    [string]$MavenRepository = "",
    [string]$ArchivoResultado = "docs/APF3/evidencias/resultado.json"
)
$ErrorActionPreference = 'Stop'
$backend = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$inicio = Get-Date
Push-Location $backend
try {
    $argumentos = @('test', '-Ddebug=false', '-Dlogging.level.org.springframework.web=INFO')
    if ($MavenRepository) { $argumentos += "-Dmaven.repo.local=$MavenRepository" }
    # Windows PowerShell 5 convierte stderr nativo en ErrorRecord. Un warning de
    # Mockito no debe abortar Maven antes de obtener su codigo de salida.
    $preferencia = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & ./mvnw.cmd @argumentos 2>&1 | ForEach-Object { "$_" }
    $codigo = $LASTEXITCODE
    $ErrorActionPreference = $preferencia
    $casos = @{}
    $total = 0; $fallidos = 0; $omitidos = 0
    Get-ChildItem 'target/surefire-reports/TEST-*.xml' -ErrorAction SilentlyContinue |
      Where-Object LastWriteTime -ge $inicio | ForEach-Object {
        [xml]$xml = Get-Content -LiteralPath $_.FullName -Raw
        foreach ($caso in $xml.testsuite.testcase) {
            $total++
            $ok = -not ($caso.SelectSingleNode('failure') -or $caso.SelectSingleNode('error') -or $caso.SelectSingleNode('skipped'))
            if (-not $ok) { $fallidos++ }
            if ($caso.SelectSingleNode('skipped')) { $omitidos++ }
            $casos[[string]$caso.name] = $ok
        }
    }
    function Resultado($id, $nombres) {
        $ok = @($nombres | Where-Object { -not $casos.ContainsKey($_) -or -not $casos[$_] }).Count -eq 0
        return [ordered]@{ id=$id; aprobado=$ok; pruebas=@($nombres) }
    }
    $reglas = @(
        (Resultado 'RN01' @('salidasConcurrentesNuncaDejanSaldoNegativo','salidaInsuficienteNoDejaCambios')),
        (Resultado 'RN02' @('transferenciaConfirmaAmbosSaldosYComparteReferencia','falloDespuesDeGuardarPrimeraMitadRevierteTodaTransferencia')),
        (Resultado 'RN03' @('aceptaMinimoCeroYMaximoMayor','rechazaMaximoIgualOMenorSinGuardar')),
        (Resultado 'RN04' @('RF04_idempotenciaYSemejanzaNoSonDuplicidadConfirmada')),
        (Resultado 'RN05' @('RF05_montoUrgenciaYUnaSolaDecision','RF05_emisionesConcurrentesCreanUnaOrden')),
        (Resultado 'RN06' @('RF03_minMaxConsumoYPendientesConResultadoConocido','RF05_cancelarNoRevierteLoYaRecibido')),
        (Resultado 'RN07' @('conteoCalculaCoincidenciaDelSaldoAlCorteYAjusteEsTrazable','conteoObsoletoRechazaInclusoSiSaldoVuelveAlMismoValor')),
        (Resultado 'RN08' @('RF05_recepcionesConcurrentesNoSuperanLoOrdenado','RF02_RF05_recepcionParcialIdempotenteYSinExceso'))
    )
    $seguridad = @(
        (Resultado 'contratos-APF2' @('todosLosContratosRestringidosRechazanTokenAusenteInvalidoYRolIndebido','loginRealYTokenAusenteInvalidoExpiradoSonRechazados')),
        (Resultado 'contratos-APF3' @('RF09_contratosNuevosRechazanSinTokenYRolIndebido','RF10_flujoComprasHttpYDashboardConPermisos'))
    )
    $flujos = @(
        (Resultado 'F01-backend' @('movimientosPersistenYAutorProvieneDeSesion','salidaInsuficienteNoDejaCambios')),
        (Resultado 'F02-backend' @('transferenciaConfirmaAmbosSaldosYComparteReferencia','falloDespuesDeGuardarPrimeraMitadRevierteTodaTransferencia')),
        (Resultado 'F03-backend' @('RF10_flujoComprasHttpYDashboardConPermisos','RF03_minMaxConsumoYPendientesConResultadoConocido')),
        (Resultado 'F04-backend' @('conteoCalculaCoincidenciaDelSaldoAlCorteYAjusteEsTrazable','conteoObsoletoRechazaInclusoSiSaldoVuelveAlMismoValor')),
        (Resultado 'F05-backend' @('RF10_flujoComprasHttpYDashboardConPermisos','RF08_I06_I08_I09FiltranPeriodoYDenominador'))
    )
    $requisitos = @(
        (Resultado 'RF01' @('registraProductoConSkuNormalizado','rechazaSkuDuplicadoSinGuardar','aceptaMinimoCeroYMaximoMayor')),
        (Resultado 'RF02' @('transferenciaConfirmaAmbosSaldosYComparteReferencia','salidasConcurrentesNuncaDejanSaldoNegativo')),
        (Resultado 'RF03' @('RF03_minMaxConsumoYPendientesConResultadoConocido')),
        (Resultado 'RF04' @('RF04_idempotenciaYSemejanzaNoSonDuplicidadConfirmada')),
        (Resultado 'RF05' @('RF05_montoUrgenciaYUnaSolaDecision','RF05_emisionesConcurrentesCreanUnaOrden')),
        (Resultado 'RF06' @('jpqlFiltraSkuAlmacenPeriodoYPagina','rotacionUsaConsumoComercialYPromedioTemporal')),
        (Resultado 'RF07' @('conteoCalculaCoincidenciaDelSaldoAlCorteYAjusteEsTrazable')),
        (Resultado 'RF08' @('indicadoresNoDuplicanProductosYPreservanPoliticaAlCorte','RF08_I06_I08_I09FiltranPeriodoYDenominador')),
        (Resultado 'RF09' @('RF09_contratosNuevosRechazanSinTokenYRolIndebido','todosLosContratosRestringidosRechazanTokenAusenteInvalidoYRolIndebido')),
        (Resultado 'RF10-backend' @('RF10_flujoComprasHttpYDashboardConPermisos'))
    )
    $contratos=@()
    if($casos['RF09_matrizDeTodosLosContratosProtegidos'] -and
        (Test-Path 'target/contratos-seguridad.json') -and
        (Get-Item 'target/contratos-seguridad.json').LastWriteTime -ge $inicio) {
        # PS 5 puede emitir el array JSON como un unico objeto al pipeline.
        $leidos=Get-Content 'target/contratos-seguridad.json' -Raw | ConvertFrom-Json
        foreach($contrato in $leidos) { $contratos += $contrato }
    }
    function Metrica($id,$lista) {
        $d=@($lista).Count
        $n=@($lista | Where-Object aprobado).Count
        $valor=if($d -eq 0){$null}else{[Math]::Round(100.0*$n/$d,2)}
        return [ordered]@{codigo=$id;numerador=$n;denominador=$d;valor=$valor;sinDatos=($d -eq 0);alcance='backend'}
    }
    $commit = (& git rev-parse HEAD).Trim()
    $huellas = Get-ChildItem src -Recurse -File | Sort-Object FullName | ForEach-Object { (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $huella = [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(($huellas -join '')))).Replace('-','').ToLowerInvariant()
    $sha.Dispose()
    $motor = if ($env:TEST_DB_URL -like 'jdbc:postgresql:*') { 'PostgreSQL (migraciones historicas tambien se prueban en H2)' } else { 'H2 en modo PostgreSQL' }
    $salida = [ordered]@{
        sinDatos=($total -eq 0); generadoEn=[DateTime]::UtcNow.ToString('o')
        version=[ordered]@{ commit=$commit; cambiosSinCommit=([bool](& git status --porcelain)); fuentesSha256=$huella }
        alcance='Backend. F01-F05 Angular y despliegue requieren evidencia adicional; no se declara aprobacion final del proyecto.'
        motor=$motor; suite=[ordered]@{ pruebas=$total; noAprobadas=$fallidos; omitidas=$omitidos; exito=($codigo -eq 0 -and $total -gt 0) }
        requisitos=$requisitos; reglas=$reglas; seguridad=$contratos; pruebasSeguridad=$seguridad; e2e=$flujos
        indicadoresTecnicos=@(
            (Metrica 'I01' $requisitos), (Metrica 'I02' $contratos),
            (Metrica 'I03' $flujos), (Metrica 'I04' $reglas)
        )
    }
    New-Item -ItemType Directory -Force (Split-Path $ArchivoResultado) | Out-Null
    $salida | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $ArchivoResultado -Encoding UTF8
    if ($codigo -ne 0 -or $total -eq 0) { throw 'La verificacion fallo; revisar resultado.json y surefire-reports.' }
} finally { Pop-Location }
