# Recorridos reales de StockGuard

revision-flujos.cjs ejecuta 20 grupos de escenarios con Chromium, Angular, Spring y PostgreSQL
reales. Se crean cinco cuentas con passwords aleatorios, productos y operaciones sinteticas.
No ejecutar contra byteelement, una base compartida o produccion. Usar una base aislada cuyo
nombre contenga test o prueba y verificar la URL JDBC del backend antes de comenzar.
confirmarBaseAislada es una confirmacion del operador: el navegador no puede consultar la
URL JDBC del servidor ni deducirla a partir de la URL de Angular.

## Preparacion

1. Node compatible con package.json, Java 21 y PostgreSQL de pruebas migrado.
2. Backend en 8080 contra esa base; Angular en http://localhost:4200 con el proxy del proyecto.
3. Administrador de pruebas con password definitivo. Si es una instalacion nueva, completar
   su primer cambio antes de ejecutar el runner.
4. Desde la raiz del repositorio, instalar herramientas solo en target:

```powershell
npm install --prefix backend/target/e2e-tools --no-save --package-lock=false playwright
node backend/target/e2e-tools/node_modules/playwright/cli.js install chromium
```

Copiar frontend/e2e/config.example.json a backend/target/e2e-config.local.json y configurar URL, nombre
de base de pruebas y credenciales privadas del Administrador. El archivo de target esta ignorado.
No modificar la plantilla versionada con contrasenas reales. No exportar el archivo privado.

## Ejecucion

Desde la raiz del repositorio:

```powershell
$env:STOCKGUARD_E2E_CONFIG = 'backend/target/e2e-config.local.json'
$env:STOCKGUARD_E2E_PLAYWRIGHT = (Resolve-Path backend/target/e2e-tools/node_modules/playwright).Path
node frontend/e2e/revision-flujos.cjs
```

STOCKGUARD_E2E_CHROMIUM permite indicar un ejecutable Chromium ya instalado. Si no se define,
Playwright usa la version descargada por su CLI. Las pruebas se ejecutan sin ventana.

Resultado, captura de indicadores y diagnosticos de fallos se escriben en backend/target.
El codigo de salida es distinto de cero si falla un escenario o hay errores JavaScript no
controlados. El runner no limpia la base: cada ejecucion crea nombres/SKU unicos. Para reiniciar
el entorno, restaurar/recrear exclusivamente la base de pruebas por un procedimiento separado.
No modifica el password de la cuenta administradora usada como acceso inicial.

## Alcance

- Primer acceso de los cinco roles, guards, token anterior revocado y ausencia de consultas
  operativas antes de cambiar password. Un password actual incorrecto conserva la sesion.
- Catalogo, politica min/max, entrada/salida, saldo insuficiente y transferencia.
- Compra urgente, similitud, duplicidad confirmada, umbral de justificacion, aprobacion/emision.
- Recepcion parcial/completa, reintento idempotente y bloqueo local por exceso.
- Conteo obsoleto, ajuste confirmado y medicion de coincidencia.
- Corte, filtros, seis indicadores y consulta comercial sin acceso a compras/Usuarios.
- Edicion concurrente, renombrado, reset, auditoria, desactivacion/reactivacion, cuenta propia
  protegida, cambio voluntario y cancelacion de pendientes sin revertir recepciones.

Las acciones se realizan con formularios/botones. Hay comprobaciones adicionales de API para
persistencia, revocacion y denegaciones. El reintento idempotente se envia directamente a la API
con el mismo cuerpo observado de la recepcion anterior. No se simulan respuestas HTTP.
La captura pasiva de clones fetch permite leer resultados aunque Chromium libere el cuerpo
original despues de cerrar un formulario; no cambia lo que recibe Angular.

Concurrencia transaccional, rollback forzado, todas las combinaciones de roles y politica de
password completa se prueban tambien en la suite Java. Estos 20 grupos no equivalen a probar
cada limite posible desde navegador. No incluyen despliegue cloud ni carga de 10000 productos.

Resultados reales de la revision del 16/11:
[revision-integrada](../../backend/docs/APF3/revision-integrada/README.md).
