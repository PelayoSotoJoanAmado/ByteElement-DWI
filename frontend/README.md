# StockGuard · Frontend

Angular 22, componentes standalone, Router y SCSS. Las dependencias están fijadas en `package-lock.json`.

## Arranque

Usar Node compatible con `package.json` (verificado con Node 24.22.1):

```bash
cd frontend
npm ci
npm start
```

Abrir http://localhost:4200. Iniciar el backend por separado en http://localhost:8080. `proxy.conf.json` reenvía `/api/**` al backend durante desarrollo. Ajustar su destino si cambia el puerto.

```bash
npm run build
npm test -- --watch=false
```

La salida de producción está en `dist/frontend/browser/`. El alojamiento debe devolver `index.html` para las rutas de la SPA y reenviar `/api/` al backend del mismo origen. No se incluyen secretos ni credenciales reales.

## Pantallas implementadas

| Ruta                    | Funciones                                                                                                  |
| ----------------------- | ---------------------------------------------------------------------------------------------------------- |
| `/`                     | Presentación pública, funcionalidades, funcionamiento y footer.                                            |
| `/login`                | Usuario/contraseña, validación, visibilidad, envío al backend, errores y estado de carga.                  |
| `/admin`                | Panel y contadores de usuarios/productos/almacenes activos y alertas de reposición, consultados en la API. |
| `/admin/usuarios`       | Listado, búsqueda, filtros por rol/estado, detalle y creación de cuentas.                                  |
| `/admin/usuarios/nuevo` | Abre directamente el formulario de creación; vuelve al listado tras crear/cancelar.                        |
| `/admin/productos`      | Listado completo, filtros, detalle, creación, edición y desactivación con confirmación.                    |
| `/admin/almacenes`      | Listado, filtros, detalle, creación, edición y desactivación con confirmación.                             |
| `/admin/existencias`    | Consulta de cantidad por producto y almacén.                                                               |
| `/admin/movimientos`    | Historial con SKU, almacén, periodo, paginación y detalle.                                                 |
| `/admin/compras`        | Reposición, solicitudes, órdenes y recepciones de cada orden; consulta con filtros/paginación/detalles.    |
| `/admin/reportes`       | Rotación y stock crítico, por periodo/SKU/almacén, paginados y con detalle.                                |
| `/admin/indicadores`    | I05–I10, con resultados, unidades, estados sin datos, errores parciales y detalle del cálculo.             |
| `/admin/evidencias`     | Archivo de resultados, fecha, alcance, versión y evidencias por categoría. No ejecuta pruebas.             |
| `/admin/estado`         | Comprobación de servicio y base de datos, fecha y tiempo de consulta.                                      |

Se conserva la estructura visual del panel original: menú, tipografía, dimensiones y colores. Las vistas usan controles, tablas, estados de petición y formularios comunes; los formularios se abren en diálogos nativos a la derecha y se adaptan a móvil. El formulario de login no tiene registro público.

## Panel de Almacén

| Ruta                                       | Funcionalidad                                                                                                                                                        |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/almacen`                                 | Inicio de operaciones, últimos cinco movimientos y accesos rápidos.                                                                                                  |
| `/almacen/existencias`                     | Consulta compartida de stock por producto y almacén.                                                                                                                 |
| `/almacen/movimientos`                     | Registrar entrada/salida; historial paginado con SKU, almacén, fechas y detalle.                                                                                     |
| `/almacen/transferencias`                  | Transferir entre almacenes activos distintos; resultado con ambos movimientos y saldos. Historial consultable en Movimientos, sin inventar un GET de transferencias. |
| `/almacen/conteos`                         | Registrar conteo (incluye cero), historial con filtros de confirmación y detalle. No permite confirmar ni ajustar.                                                   |
| `/almacen/recepciones`                     | Órdenes emitidas pendientes, selección por detalle o ID, producto/almacén de destino, recepción parcial/completa e historial paginado por orden.                     |
| `/almacen/productos`, `/almacen/almacenes` | Catálogos compartidos de solo lectura, búsqueda, filtros y detalle.                                                                                                  |
| `/almacen/reposicion`, `/almacen/compras`  | Consultas compartidas de reposición, solicitudes y órdenes; enlace desde la orden a su recepción.                                                                    |
| `/almacen/reportes`                        | Reportes compartidos de rotación y stock crítico.                                                                                                                    |

Se reutilizan la estructura visual, navegación, footer, componentes de catálogos, consultas, tablas y cliente HTTP del administrador. La selección de acciones depende del rol; la API conserva la autorización y todas las reglas de negocio. No se modifica el backend.

Las cantidades se validan como enteros seguros (positivos o cero para conteos); se exige motivo y producto/almacén activos para operar. Los errores de stock insuficiente y conflictos se muestran con el mensaje del servidor. No se permite doble envío mientras la petición está pendiente. Tras éxito, el formulario queda bloqueado hasta iniciar otra operación.

Cada recepción conserva su clave de idempotencia y contenido al reintentar, incluso al recargar en la misma pestaña mediante `sessionStorage` por usuario. Ante resultado incierto, solo la recepción permite reenviar exactamente la misma petición; movimientos, transferencias y conteos deben revisarse en el historial antes de iniciar otra operación, porque sus endpoints no admiten claves de idempotencia. No se realizan reintentos automáticos. La recuperación depende de que el almacenamiento de sesión esté disponible.

## Permisos y sesión

El área `/admin` requiere una sesión vigente con rol `ADMINISTRADOR`. El área `/almacen` y todas sus rutas hijas requieren `ALMACEN`. El login redirige cada uno a su panel; sus menús no permiten saltar de rol. Los otros tres roles aún conservan la presentación pública como destino tras el login.

El interceptor adjunta Bearer solo a rutas relativas `/api/v1/`, excluyendo login. Un 401 de la sesión actual la cierra; un 403 no la elimina y un 401 tardío de una sesión anterior no cierra una sesión nueva. El panel vigila el vencimiento, y cerrar sesión elimina el token y vuelve al login.

El token, usuario y vencimiento se guardan en `sessionStorage` (en memoria si el navegador bloquea almacenamiento). Nunca se guarda la contraseña en la sesión. Los controles del cliente complementan la autorización real y la validación del JWT del servidor.

La interfaz respeta los cinco roles del backend: `ALMACEN`, `COMPRAS`, `SUPERVISOR`, `CONSULTA_COMERCIAL`, `ADMINISTRADOR`.

## Límites y reglas del backend respetados

- Usuarios: solo listar/crear. No hay edición, desactivación ni cambio de contraseña en la API actual.
- Productos y almacenes: desactivación lógica; sin borrado definitivo ni reactivación no soportada.
- Compras y movimientos son de consulta para el administrador. No hay botones para aprobar, emitir órdenes, recibir, generar cortes o registrar movimientos, que pertenecen a otros roles.
- Existencias requiere un producto y un almacén: no se simula un endpoint de stock global.
- La API de productos devuelve arrays paginados sin totales. Se recorren páginas de 100 hasta completarlas para búsquedas, selectores y conteos correctos. Usuarios/almacenes se reciben completos. Los filtros de estos listados y su paginación de 20 filas son locales. Un catálogo muy grande podría justificar ampliar el contrato con totales/búsqueda en servidor; no se truncan resultados silenciosamente.
- Movimientos, compras y reportes usan paginación real del servidor, filtros y totales.
- Las fechas elegidas son días de Lima; la fecha final incluye el día completo y se convierte al instante exclusivo del día siguiente, conforme al contrato.
- Indicadores nulos/sin datos muestran «— / Sin datos», nunca 0 %. I08 usa días; los demás porcentajes. El fallo de un indicador no oculta los otros cinco.
- Stock crítico y algunos indicadores dependen de cortes/políticas del supervisor. Alertas de reposición solo cubre los pares con políticas configuradas. No se crean datos ficticios para llenar el panel.
- Las evidencias muestran el alcance y motor registrados en el archivo; no prueban automáticamente el estado actual ni el frontend.

## Organización

- `src/app/core/auth/`: login, sesión, guards e interceptor.
- `src/app/pages/admin/layout/` y `sidebar/`: estructura y navegación compartidas por rol.
- `src/app/pages/warehouse/`: inicio de Almacén, operaciones, navegación y estilos de esa área.
- `src/app/pages/admin/dashboard/`: resumen conectado a la API.
- `src/app/pages/admin/resources/`: cuentas y catálogos.
- `src/app/pages/admin/queries/`: inventario, compras, reportes e indicadores.
- `src/app/pages/admin/system/`: salud y evidencias.
- `src/app/pages/admin/shared/`: cliente HTTP, formatos de fecha, tablas, detalles y estados de peticiones.
- `src/styles/_admin-modules.scss`: estilos comunes, limitados al área administrativa.
- `public/images/`: fondos e ilustración originales (ver `docs/IMAGENES.md`).

## Verificación

La entrega anterior del administrador se verificó con compilación y 53 pruebas automatizadas. Esas pruebas cubren protección por los cinco roles, sesión, login, creación y validación de usuarios, confirmación de contraseñas, máximo BCrypt de 72 bytes UTF-8, contratos de catálogo y paginación completa, fechas inclusive/exclusiva en Lima, consultas de stock cero, indicadores sin datos/errores parciales y estados de salud/evidencias.

Las respuestas de la API se verifican con HttpTestingController o servicios de prueba. La revisión visual en navegador usa HTML renderizado por los componentes Angular con datos de prueba; esos fixtures no forman parte de la aplicación ni agregan excepciones a los guards.

Para esta ampliación de Almacén se verifica la compilación de producción. La ejecución de pruebas se deja para la etapa final solicitada por el usuario; se actualizan las expectativas existentes de redirección y los fixtures de permisos.

Queda pendiente validar los nuevos flujos de extremo a extremo con el backend/PostgreSQL del usuario. No se modificó el backend ni se crearon usuarios/productos/almacenes reales en esta entrega. Ver `docs/VALIDACION-ADMIN.md` para la comprobación local.
