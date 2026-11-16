# Integración Angular V8: implementación local y resultados

Corte historico de implementacion, anterior a la revision local con PostgreSQL. Los bloqueos
y pendientes E2E registrados abajo se contrastan con la [revision integrada posterior](../../backend/docs/APF3/revision-integrada/README.md),
que ejecuto 20 grupos reales y 94 pruebas backend por motor. Conservar la fecha y alcance de cada evidencia.

Fecha: 16 de noviembre de 2026. Base revisada: rama `front`, commit `e63f52e`.
El usuario autorizó organizar los cambios en cuatro commits y publicarlos en la rama `front`.
Este documento actualiza el estado de implementación; no acredita recorridos E2E con PostgreSQL.

## Cambios implementados

- Modelos V8: `requiereCambioPassword`, `version` y eventos de cuenta.
- Restauración de sesión mediante `GET /auth/me`, antes de activar módulos. Se utiliza el rol y estado devueltos por el servidor, con consulta compartida para guards concurrentes. Una sesión anterior a V8 sin los campos requeridos se descarta.
- `/cuenta/cambiar-password`: pantalla común de Mi cuenta para los cinco roles. Cambio obligatorio antes de cualquier panel y cambio voluntario desde la cabecera. Validación de confirmación, contraseña distinta y límite UTF-8 mediante TextEncoder; no se recortan las claves.
- Login y cambio propio aplican el mismo reemplazo de sesión: token nuevo, usuario nuevo y expiración nueva, tanto en memoria como en sessionStorage. Las contraseñas no se almacenan allí. El formulario se limpia al finalizar correctamente.
- Guards de rol para las cinco áreas. `/acceso-denegado` muestra un rechazo por permisos sin cerrar una sesión válida.
- Interceptor: `CAMBIO_PASSWORD_REQUERIDO` conduce al cambio; un 403 ordinario conduce a acceso denegado; un 401 del token actual cierra la sesión. Los errores tardíos de otro token no afectan a la sesión nueva.
- Usuarios: desde Detalle se accede a edición de nombre, rol y estado, con motivo y versión original; la desactivación requiere confirmación. El reset temporal tiene confirmación, motivo y versión. Historial paginado de eventos. Los conflictos conservan el error y no provocan reintentos con otra versión. La propia cuenta solo ofrece su historial y la indicación de utilizar Mi cuenta.
- Compras: reposición con detalle de regla/consumo/pendientes, preparación de solicitud, monto y urgencia, envío, similares con navegación a su detalle, emisión desde solicitud aprobada y consulta de recepciones desde la orden. Se conserva la clave idempotente al reintentar una petición fallida y se impide cambiar silenciosamente su contenido tras el primer envío.
- Supervisor: consultas compartidas, justificación de aprobación, rechazo y duplicidad con motivo/original; cancelación del pendiente de órdenes; políticas min/max; conteos y confirmación de ajuste; generación y consulta de cortes por lote conocido; seis indicadores, con datos de cálculo y estados Sin datos; estado del servicio.
- Consulta Comercial: catálogo, existencias, movimientos y reportes de inventario, sin compras internas ni indicadores.
- Administrador conserva catálogo/cuentas/consultas y agrega consulta de cortes conocidos. Almacén conserva movimientos, transferencias, conteos y recepciones. En las áreas existentes, Compras agrupa las subpestañas de reposición/solicitudes/órdenes; recepciones se consultan en el detalle de órdenes, como permite la matriz.
- No se modifica el backend, sus permisos, las migraciones ni la provisión inicial. No se agrega SUPERADMIN ni se crean cuentas iniciales desde Angular.

## Comprobaciones ejecutadas

Ejecutadas desde `frontend`:

| Comando | Resultado observado | Evidencia |
|---|---|---|
| `npm ci` | Correcto; 299 paquetes instalados | Dependencias del lock existente; no se modificó package-lock |
| `npm test -- --watch=false` | 12 archivos, 97 pruebas aprobadas; 0 fallidas | [angular-tests.log](evidencias-integracion/angular-tests.log) |
| `npm run build` | Compilación de producción correcta | [angular-build.log](evidencias-integracion/angular-build.log) |
| `npx tsc --noEmit -p tsconfig.spec.json` | Código de salida 0; sin errores TypeScript | [typescript.log](evidencias-integracion/typescript.log) |
| `git diff --check` | Sin errores de espacios | Revisión local del diff |

Las salidas guardadas solo normalizan espacios finales y líneas vacías al cierre; no cambian los resultados.

Las pruebas Angular usan HttpTestingController, RouterTestingHarness o dobles de AdminApi.
Comprueban formularios, rutas, métodos, payloads y manejo de respuestas simuladas. No levantan
Spring, no persisten datos y no demuestran la autorización ni revocación reales del servidor.

Se intentó `./mvnw test -q` desde `backend`: falló antes de compilar porque no se pudo resolver
`repo.maven.apache.org` para descargar `spring-boot-starter-parent:4.1.1`. No se cambió la versión
para sortear ese bloqueo. [Salida real](evidencias-integracion/backend-bloqueado.log).
No se dispone de PostgreSQL, psql o Docker en este entorno, ni de una conexión a una base de pruebas.
No se arrancó el backend ni se modificó ninguna base existente.

Se intentó una revisión visual con Chromium/Playwright y respuestas HTTP simuladas. Chromium
no pudo arrancar: `socket() failed: Operation not permitted`. No se obtuvo validación visual
ni evidencia de navegador. Este intento tampoco habría acreditado integración con el backend.

## Los doce escenarios del contrato de cuentas

“Cobertura Angular” indica comprobaciones con respuestas simuladas, no un resultado E2E.
Todos los escenarios de la última columna siguen pendientes con backend y PostgreSQL de pruebas.

| # | Escenario | Cobertura Angular ejecutada | Validación real pendiente |
|---|---|---|---|
| 1 | Cambio pendiente por cada rol y URL directa | Cinco roles, login y rutas privadas; la respuesta de /me reemplaza un flag local alterado | Iniciar con las cinco cuentas reales y entrar por URL directa |
| 2 | Ninguna petición de panel antes del cambio | Se comprueba que no se monta el layout ni se envían peticiones de módulo | Registrar tráfico HTTP real antes/después del primer cambio |
| 3 | Actual incorrecta, mismatch y más de 72 bytes | Validación local y 400 simulado conservando sesión | Probar política, comparación BCrypt y mensajes reales |
| 4 | Reemplazo del token y permisos del rol | Token/usuario/expiración/storage reemplazados; rutas propias y catálogos de consulta | Verificar rechazo del token anterior y acceso del nuevo por rol |
| 5 | Cambio voluntario y 401 viejo | Formulario voluntario; limpieza de campos; 401 anterior no borra sesión nueva | Peticiones concurrentes reales durante la rotación |
| 6 | Administrador crea cada rol | Formulario probado para los cinco roles; payload sin confirmación | Crear, iniciar sesión y cambiar contraseña de cada cuenta nueva |
| 7 | Edición versionada y conflicto 409 | Payload con versión recibida/motivo; 409 visible, sin reintento automático | Dos administradores editando el mismo registro |
| 8 | Desactivar/reactivar y revocación | Formulario de estado, motivo y confirmación de desactivación | Probar login denegado, reactivación y token viejo inválido |
| 9 | Reset, revocación y secretos | Payload temporal con motivo/versión y limpieza posterior; tabla sin claves | Login temporal, cambio obligatorio, auditoría y revocación real |
| 10 | Cuenta propia protegida y otros roles sin Usuarios | Se bloquean edición/reset propios; los roles ajenos no abren /admin; historial propio permitido | Intentos HTTP directos sobre la propia cuenta y Usuarios con otro rol |
| 11 | Acciones indebidas 403 y sin registro público | Separación de rutas/acciones y tratamiento de 403 ordinario; no se añade ruta/API de registro | Matriz negativa real de endpoints con los cinco roles |
| 12 | E2E con backend y PostgreSQL | No ejecutado | Ejecutar íntegramente en entorno aislado antes de declarar el contrato validado |

Pruebas principales: `accounts-contract.spec.ts`, `admin-access.spec.ts`,
`auth.interceptor.spec.ts`, `login-form.spec.ts`, `admin-resources.spec.ts`
y `role-operations.spec.ts`. Las pruebas previas de catálogo, consultas, API y paneles
se mantienen en la suite ejecutada.

## Flujos F01–F05

| Flujo | Recorrido a validar en Angular con persistencia real | Estado de esta revisión |
|---|---|---|
| F01 | Almacén registra entrada/salida; comprobar saldos, autor de sesión y rechazo de saldo insuficiente sin escritura parcial | Pendiente E2E; se conserva la interfaz existente |
| F02 | Almacén transfiere; comprobar ambos saldos, referencia compartida y reversión ante fallo | Pendiente E2E; se conserva la interfaz existente |
| F03 | Supervisor configura política → Compras consulta reposición y solicita → Supervisor decide → Compras emite → Almacén recibe parcial/completo; consultar similares, urgencia e idempotencia | Interfaces conectadas en código; pruebas aisladas de acciones; pendiente recorrido real completo |
| F04 | Almacén registra conteo → Supervisor compara y confirma → revisar ajuste/movimiento; provocar conteo obsoleto y 409 | Interfaz de confirmación agregada; conflicto simulado probado; pendiente recorrido real |
| F05 | Supervisor genera corte → consulta de lote y dashboard/indicadores/reportes con periodo de Lima, numeradores, denominadores, unidades y Sin datos | Interfaces conectadas en código; pruebas aisladas de corte/políticas/indicadores; pendiente recorrido real |

No se reutilizan los resultados históricos `Fxx-backend` del repositorio como si fueran evidencia
actual de navegador o pruebas ejecutadas en esta revisión.

## Para completar la aceptación

1. Preparar un PostgreSQL exclusivo de pruebas, conexión privada y Java 21 con acceso a Maven Central. No usar la base personal/de producción para estos recorridos.
2. Arrancar el backend según cuentas/README.md; dejar que él provisione las cinco cuentas en la base vacía, con cinco claves privadas distintas. Angular no las crea. No guardar claves en este informe o en Git.
3. Ejecutar la suite del backend. Arrancar Angular con su proxy existente hacia el backend local.
4. Recorrer los doce escenarios y F01–F05 desde navegador, alternando usuarios reales. Registrar código de versión, entorno, resultados y evidencia sin credenciales.
5. Actualizar este documento con los resultados observados. Resolver cualquier diferencia visual o de integración antes de declarar la aceptación final.

La publicación de estos cambios no equivale a validación completa del proyecto.
