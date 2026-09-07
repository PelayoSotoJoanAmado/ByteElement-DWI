# StockGuard MYPE: API y trazabilidad de APF1

Para la instalación del equipo, el origen de los datos y la explicación de conceptos, consultar la [guía completa de cierre](CIERRE-APF1-GUIA-COMPLETA.md).

Estado verificado el 7 de septiembre de 2026. Fuente de requisitos: `docs/AvanceProyectoDWI (3).docx`, sección 2.1; alcance por avance: ruta del documento docente, APF1 y APF2.

## Qué se puede demostrar

La API tiene 17 contratos HTTP. RF01 dispone ahora de creación, consulta, edición y desactivación de productos y almacenes, y configuración y consulta de políticas mínimo/máximo por producto y almacén. RF02 tiene un flujo de demostración de entradas, salidas y transferencias en memoria.

Hay tres entidades JPA: **Producto, Almacen y PoliticaStock**. Sus tablas se crean con Flyway; Hibernate las valida. `Movimiento` sigue siendo un modelo Java sin persistencia y `TipoMovimiento` es un enum. Un DTO representa la entrada o salida HTTP; no necesita `@Entity`.

**Alcance pendiente:** no se declara aceptación total de RF01/RF02 ni cumplimiento de RF09. Faltan permisos por actor, persistencia del inventario y su historial, control transaccional en PostgreSQL y versiones históricas de las políticas para cortes e indicadores. El catálogo guarda la política actual; editarla no conserva sus valores anteriores. El historial de movimientos permanece consultable al desactivar durante la misma ejecución, pero se pierde al reiniciar.

## Requisito → endpoint → código → prueba

| Parte del requisito | Endpoint | Dónde está la aplicación de la regla | Prueba |
|---|---|---|---|
| RF01: crear producto con SKU único | POST /api/v1/productos | ProductoService.crear; ProductoRepository.existsBySku; V1: uk_producto_sku | ProductoServiceTest.rechazaSkuDuplicadoSinGuardar |
| RF01: editar datos del producto | PUT /api/v1/productos/{id} | ProductoService.actualizar; Producto.actualizar; consulta de SKU excluyendo el ID propio | ProductoServiceTest.edicionRechazaSkuDeOtroProductoSinModificarElOriginal |
| RF01: conservar registros al desactivar | PATCH /api/v1/productos/{id}/desactivar | ProductoService.desactivar; Producto.desactivar; no se invoca delete | ProductoServiceTest.desactivarConservaElProductoYEsRepetible |
| RF01: administrar almacenes | POST /api/v1/almacenes; PUT /api/v1/almacenes/{id} | AlmacenController; AlmacenService; AlmacenRepository; entidad Almacen | AlmacenServiceTest: crear, editar y rechazar código duplicado |
| RF01: conservar almacenes al desactivar | PATCH /api/v1/almacenes/{id}/desactivar | AlmacenService.desactivar; Almacen.activo | AlmacenServiceTest.desactivarConservaRegistro |
| RF01: mínimo >= 0 y máximo > mínimo | PUT /api/v1/productos/{productoId}/almacenes/{almacenId}/politica-stock | PoliticaStockService.guardar; PoliticaStock.actualizar; V2: ck_politica_limites | PoliticaStockServiceTest: mínimo cero, negativo, máximo igual/menor y actualización |
| RF01: consultar la política del par | GET /api/v1/productos/{productoId}/almacenes/{almacenId}/politica-stock | PoliticaStockRepository.findByProductoIdAndAlmacenId; PoliticaStockResponse | Verificar-RF01.ps1: consulta y límites conservados tras un rechazo |
| RF02: entradas y salidas con autor, fecha y motivo | POST /api/v1/movimientos | InventarioService.registrar; Movimiento | InventarioServiceTest.entradaIncrementaSaldoYConservaDatosDelMovimiento |
| RF02: impedir stock negativo | POST /api/v1/movimientos, tipo SALIDA | InventarioService.registrar lanza StockInsuficienteException antes de modificar saldos | InventarioServiceTest.salidaExcesivaNoModificaSaldoNiRegistraMovimiento |
| RF02: ambos efectos de transferencia | POST /api/v1/transferencias | InventarioService.transferir, sincronizado dentro de una instancia Java | InventarioServiceTest.transferenciaConSaldoInsuficienteRechazaSinAlterarSaldos |
| Consulta inicial relacionada con RF06 | GET /api/v1/movimientos?productoId=...&almacenId=... | InventarioService.listarMovimientos, filtros en memoria | InventarioControllerTest.listarMovimientosRetorna200 |

RF06 está solo iniciado: faltan JPQL, filtros por período, paginación de movimientos, rotación y stock crítico.

Los tamaños de campos, normalización de códigos, ruta de política y respuesta 409 para registros desactivados son decisiones técnicas de este prototipo. El Word exige conservar registros con historial; aquí se aplica baja lógica a todos los productos y almacenes y no se expone borrado físico. Los registros inactivos conservan sus consultas, pero no admiten nuevos movimientos ni cambios de política. No se implementó reactivación en este bloque.

## Todos los endpoints

Todas las rutas usan `/api/v1`.

| Método | Ruta relativa | Respuesta satisfactoria |
|---|---|---|
| POST | /productos | 201 + Location |
| GET | /productos?pagina=0&tamanio=20 | 200, lista paginada |
| GET | /productos/{id} | 200 |
| PUT | /productos/{id} | 200 |
| PATCH | /productos/{id}/desactivar | 200 |
| GET | /almacenes | 200 |
| GET | /almacenes/{id} | 200 |
| POST | /almacenes | 201 + Location |
| PUT | /almacenes/{id} | 200 |
| PATCH | /almacenes/{id}/desactivar | 200 |
| GET | /productos/{productoId}/almacenes/{almacenId}/politica-stock | 200 |
| PUT | /productos/{productoId}/almacenes/{almacenId}/politica-stock | 200, crea o sustituye los límites actuales del mismo par |
| POST | /movimientos | 201 + Location |
| GET | /movimientos?productoId=...&almacenId=... | 200 |
| GET | /movimientos/{id} | 200 |
| POST | /transferencias | 201 + Location del movimiento de salida |
| GET | /existencias?productoId=...&almacenId=... | 200 |

Errores demostrados: **400** por campos/límites inválidos, **404** por recurso inexistente, **409** por duplicados, stock insuficiente o uso operativo de registros inactivos. Los listados de productos/almacenes incluyen registros inactivos para permitir su consulta. No existe autenticación todavía.

PUT de producto requiere todos los campos del DTO `CrearProductoRequest`, reutilizado como contrato de escritura para evitar duplicar validaciones. PUT de almacén requiere código y nombre. La desactivación no lleva cuerpo y puede repetirse.

## Estructura del código

Dentro de `src/main/java/com/byteelement/stockguard`:

- `controller`: ProductoController, AlmacenController, PoliticaStockController, InventarioController y ApiExceptionHandler. Las consultas de almacenes existentes siguen atendidas desde InventarioController, que delega al catálogo persistido.
- `dto`: contratos Request/Response; validaciones de entrada con Jakarta Validation.
- `service`: ProductoService, AlmacenService, PoliticaStockService e InventarioService.
- `entity`: Producto, Almacen y PoliticaStock con JPA; Movimiento sin JPA y TipoMovimiento como enum.
- `repository`: ProductoRepository, AlmacenRepository y PoliticaStockRepository.
- `exception`: excepciones de SKU duplicado, stock insuficiente y conflicto de catálogo.

Recorrido del catálogo: **HTTP → controller → service → repository → PostgreSQL**.

Recorrido del inventario: **HTTP → InventarioController → InventarioService → mapas en memoria**. La validación de productos y almacenes consulta los servicios de catálogo persistido.

PoliticaStock tiene dos relaciones `@ManyToOne`: una hacia Producto y otra hacia Almacen. La restricción UNIQUE del par evita dos políticas actuales para el mismo producto/almacén. El stock sigue perteneciendo al par y no se añadió como columna a Producto.

## Demostración en clase

Ejecutar siempre desde `backend`. Las dependencias deben estar descargadas antes de la exposición.

### 1. Mostrar pruebas sin PostgreSQL

```powershell
.\mvnw.cmd "-Dtest=ProductoServiceTest,AlmacenServiceTest,PoliticaStockServiceTest,InventarioServiceTest,InventarioControllerTest,CatalogoControllerTest" test
```

Son **50 pruebas** de servicios y contratos HTTP con Mockito/MockMvc, sin servidor HTTP real. Para mostrar solo la regla de límites:

```powershell
.\mvnw.cmd "-Dtest=PoliticaStockServiceTest" test
```

### 2. Suite completa con PostgreSQL

```powershell
.\mvnw.cmd test
```

Son **51 pruebas**, incluida StockguardApplicationTests. La prueba de contexto utiliza PostgreSQL local y aplica las migraciones pendientes.

Si aparece PKIX en este Windows, se verificó el uso del almacén de certificados del sistema sin deshabilitar TLS:

```powershell
.\mvnw.cmd "-Djavax.net.ssl.trustStoreType=Windows-ROOT" "-Djavax.net.ssl.trustStore=NUL" test
```

### 3. Demostrar los endpoints reales

Iniciar la API con PostgreSQL disponible:

```powershell
.\mvnw.cmd spring-boot:run
```

En otra terminal dentro de backend:

```powershell
.\docs\Verificar-RF01.ps1
.\docs\Verificar-APF1.ps1
```

El primero prueba catálogo y políticas, incluida una entrada en un almacén nuevo, desactivación y conservación de consultas. El segundo demuestra entradas, salidas y transferencias. Ambos crean productos QA persistentes; el primero también crea un almacén y una política, y deja producto/almacén desactivados al finalizar.

Para exponer manualmente, importar `StockGuard-RF01.postman_collection.json` en Postman y ejecutar en orden. Contiene 18 solicitudes con aserciones y genera SKU/código únicos por ejecución. La colección previa `StockGuard-APF1.postman_collection.json` conserva la demostración del inventario. La validación HTTP de esta revisión se ejecutó con PowerShell; no se ejecutó el runner de Postman.

Secuencia sugerida de exposición:

1. Mostrar RF01 en el Word y señalar la fila correspondiente de esta matriz.
2. Crear y editar un producto; intentar duplicar su SKU y explicar el 409.
3. Crear un almacén y mostrar su fila persistida.
4. Configurar mínimo 0/máximo 10; probar mínimo -1 y máximo igual al mínimo: 400.
5. Abrir PoliticaStockService y su test: señalar la condición del requisito.
6. Registrar una entrada, desactivar el catálogo y comprobar que los registros siguen consultables.
7. Mostrar las pruebas aprobadas y explicar los límites de esta versión.

## Evidencia observada

- `tdd-rf01-politicas-rojo.log`: 4 pruebas antes de implementar guardar política, con 2 fallos y 2 errores por el método provisional.
- `tdd-rf01-verde.log`: suite completa, 51 pruebas aprobadas, 0 fallos y 0 errores.
- `verificacion-rf01-http.log`: 30 peticiones HTTP aprobadas.
- `verificacion-apf1-tras-rf01.log`: 22 peticiones del flujo anterior aprobadas.
- Flyway registra V1 y V2 exitosas. V1 no fue modificada.
- PostgreSQL conservó la política QA ID 1 (producto 3, almacén 4), mínimo 2/máximo 20, junto a producto y almacén inactivos.
- Tras reiniciar la API se consultaron esos tres recursos por HTTP: mantuvieron los límites y estados. Flyway informó que el esquema estaba actualizado y no volvió a aplicar V2. El reinicio vació los mapas de inventario, como corresponde al alcance actual.

Distribución: ProductoServiceTest 5, AlmacenServiceTest 5, PoliticaStockServiceTest 4, InventarioServiceTest 17, InventarioControllerTest 13, CatalogoControllerTest 6 y StockguardApplicationTests 1.

Los logs anteriores `tdd-apf1-correcciones-*.log` y `tdd-inventario-rojo.log` son evidencia histórica de bloques anteriores; sus conteos no representan la suite actual. No se afirma TDD retroactivo para todo el código.

## Pendientes del informe y próximos avances

| Requisito | Estado de esta versión |
|---|---|
| RF01 | Catálogo y límites implementados; permisos y alcance histórico completo pendientes |
| RF02 | Flujo API en memoria; persistencia, auditoría duradera y transacciones PostgreSQL pendientes |
| RF03 | Reposición, consumo reciente y pedidos pendientes no implementados |
| RF04 | Solicitudes, similitudes e idempotencia no implementadas |
| RF05 | Aprobaciones y órdenes no implementadas |
| RF06 | Consulta básica de movimientos; JPQL y reportes pendientes |
| RF07 | Conteos y ajustes no implementados |
| RF08 | Indicadores I05–I10 no calculados |
| RF09 | JWT y permisos no implementados; autor recibido como texto de demostración |
| RF10 | Integración con frontend y reportes de evidencias por versión pendientes |

Alinear la sección 2.2 del Word con estas clases. En 2.3, el ejemplo de TransferenciaController debe indicar InventarioController para describir la implementación actual. En 2.4, distinguir catálogo persistido de inventario en memoria. Completar los diagramas 2.5–2.7 y la sección TDD 2.8 con evidencia real. Los endpoints de indicadores/evidencias del Word son contratos planificados y deben conservar esa etiqueta.

El docente separa API/TDD inicial (APF1) de persistencia, JPQL, transacciones y seguridad (APF2). Esta ampliación añade persistencia del catálogo como soporte de los endpoints solicitados, sin declarar terminado el backend del curso. Los roles del informe siguen siendo requisitos pendientes; un endpoint sin seguridad no acredita su cumplimiento.
