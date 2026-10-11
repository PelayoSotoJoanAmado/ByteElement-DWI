# APF2: backend seguro y persistente

Rama de trabajo: `prueba`. Alcance: cubrir la puerta de calidad APF2 del documento docente (modelo relacional, JPA/Hibernate, CRUD, JPQL, transacciones, Spring Security, roles, JWT, datos de prueba e indicadores calculables desde persistencia).

Esta entrega no declara terminado el proyecto completo: I06, I08 e I09 dependen de solicitudes/órdenes de compra y quedan pendientes, junto a RF03–RF05, frontend, evidencias técnicas RF10, restauración y despliegue público. RF08 queda parcial con I05, I07 e I10. Los logs y colecciones APF1 son históricos: sus peticiones sin token ya no son válidas para la rama `prueba`.

## Ejecutar

Requisitos: Java 21, PostgreSQL 16 y Maven Wrapper. No se incluyen credenciales reales.

1. Definir `DB_PASSWORD` y ejecutar `docker compose up -d db` desde la raíz, o usar una base PostgreSQL propia.
2. Desde `backend`, copiar `application-local.properties.example` a `application-local.properties` y reemplazar todos los valores `REEMPLAZAR`. También se pueden usar `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `BOOTSTRAP_USER`, `BOOTSTRAP_PASSWORD` y `DEMO_PASSWORD` como variables de entorno.
3. Ejecutar `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`). La primera ejecución crea un administrador únicamente si no existen usuarios. Su contraseña requiere al menos 12 caracteres y no más de 72 bytes. JWT_SECRET debe ser aleatoria y tener al menos 32 bytes.
4. Para una base de exposición, ejecutar `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`. El perfil optativo genera usuarios `demo-almacen`, `demo-compras`, `demo-supervisor`, `demo-consulta_comercial` y `demo-administrador`, usando DEMO_PASSWORD. También genera DEMO-RAM y DEMO-SSD, sus políticas, movimientos, conteos y un corte. Su carga es transaccional y no repite las entradas al reiniciar. Utilizar una base separada para la demostración.

Flyway conserva V1/V2 y añade V3; Hibernate valida el esquema. Los movimientos que existían únicamente en memoria en la versión APF1 no pueden migrarse: no existe registro duradero de ellos. Los saldos empiezan en cero hasta registrar entradas reales o cargar el perfil demo.

## Modelo y transacciones

Tablas: `producto`, `almacen`, `politica_stock`, `usuario`, `existencia`, `movimiento`, `conteo`, `corte_stock`.

- Existencia tiene un único saldo por producto/almacén, CHECK cantidad >= 0 y `@Version`.
- Cada operación bloquea primero el producto mediante `PESSIMISTIC_WRITE`. Esto también protege el primer saldo de un par aún inexistente y serializa el mismo SKU entre instancias. A continuación bloquea almacenes por ID ascendente. Se prioriza consistencia sobre concurrencia entre almacenes del mismo producto.
- Entrada, salida y transferencia operan dentro de `@Transactional`. Transferencia guarda dos movimientos con la misma referencia UUID y fecha; un fallo revierte ambos saldos y registros.
- El actor se obtiene del contexto autenticado. `autor` en los DTO antiguos es opcional y se ignora; no permite suplantar usuarios.
- Las ediciones/desactivaciones y las políticas comparten el bloqueo del catálogo. La baja lógica conserva saldos y movimientos consultables y rechaza nuevas operaciones del recurso inactivo.
- Conteo conserva saldo, cantidad física, fecha, actor, motivo y último ID de movimiento al corte. Solo supervisor confirma; una segunda confirmación o cualquier movimiento posterior invalida el ajuste. Un ajuste con diferencia genera un movimiento vinculado; una coincidencia se confirma sin inventar movimiento de cantidad cero.
- CorteStock fotografía todos los pares de productos activos y almacenes habilitados, incluso saldos cero. Conserva SKU, condición crítica y mínimo/máximo vigentes. Cambiar posteriormente una política no altera el corte ya generado.

## Permisos

La matriz sigue el informe del repositorio. Administrador no hereda automáticamente los permisos de almacén o supervisor.

| Acción | Almacén | Compras | Supervisor | Consulta comercial | Administrador |
|---|---|---|---|---|---|
| Leer catálogo, existencias, movimientos y reportes | Sí | Sí | Sí | Sí | Sí |
| Mantener productos/almacenes y crear/listar usuarios | No | No | No | No | Sí |
| Crear/editar políticas | No | No | Sí | No | No |
| Registrar entradas, salidas, transferencias y conteos | Sí | No | No | No | No |
| Leer conteos | Sí | No | Sí | No | No |
| Confirmar ajustes y generar cortes | No | No | Sí | No | No |
| Leer indicadores y cortes | No | No | Sí | No | Sí |

Solo `POST /api/v1/auth/login` es público. Las rutas no previstas se deniegan. Sin token o con JWT inválido/expirado se devuelve 401; rol indebido devuelve 403. Las contraseñas se almacenan con BCrypt. JWT usa HS256, emisor validado y vencimiento de 30 minutos; se verifica además usuario activo y rol actual en la base. Los endpoints de usuarios nunca retornan el hash.

## Contratos nuevos y modificados

Todos usan `/api/v1`. Fechas: ISO 8601 con zona, por ejemplo `2026-10-11T00:00:00Z`; todos los períodos son `[desde,hasta)`: hasta exclusivo.

| Método y ruta | Uso |
|---|---|
| POST /auth/login | `{ "username": "...", "password": "..." }`; retorna accessToken, tokenType, expiresIn y usuario |
| POST /usuarios | `{ "username": "...", "password": "...", "rol": "ALMACEN" }`; solo administrador |
| GET /usuarios | Usuarios sin contraseñas |
| GET /movimientos | productoId, sku, almacenId, desde, hasta, pagina, tamanio; respuesta Page con content y totalElements |
| POST /conteos | productoId, almacenId, cantidadFisica, motivo; 201 |
| GET /conteos/{id} | Corte físico y estado de confirmación |
| POST /conteos/{id}/confirmar | Ajuste exclusivo del supervisor; 409 si obsoleto o confirmado |
| POST /cortes-stock | Genera un lote coherente con saldos y políticas, retorna sus filas |
| GET /cortes-stock/{lote} | Consulta la fotografía conservada |
| GET /reportes/rotacion | SKU, almacén, período, pagina, tamanio |
| GET /reportes/stock-critico | SKU, almacén, período, pagina, tamanio; requiere corte en el período |
| GET /indicadores | codigo=I05/I07/I10, desde, hasta; filtros opcionales sku y almacenId |

Se mantienen los contratos de catálogo, políticas, movimientos, transferencias y consulta de existencias. GET /movimientos cambia de lista simple a objeto paginado; actualizar consumidores antiguos para leer `content`. pagina >= 0, tamanio entre 1 y 100. Las consultas de movimientos, períodos, últimos saldos y cortes emplean JPQL declarado en los repositorios; agrupación y paginación de reportes se completan en el servicio para este volumen de exposición.

## Fórmulas y límites de evaluación

- **I05**: productos críticos con saldo cero en algún almacén / productos críticos evaluados × 100. Se cuenta el producto una vez. La población se toma del último corte completo anterior a hasta y dentro del período; el saldo al inicio se reconstruye con el último movimiento previo (o cero cuando no existe) y se revisan todos los movimientos del intervalo. Para llamarlo “quiebre semanal”, seleccionar exactamente una semana. Evaluar con catálogo/almacenes constantes durante el período: no se versionan todavía altas, desactivaciones ni la condición crítica del catálogo.
- **I07**: observaciones de conteo físico iguales al saldo conservado al corte / observaciones válidas × 100. Cambios posteriores del stock no cambian el resultado. Un conteo repetido corresponde a otra observación temporal.
- **I10**: productos críticos que superan el máximo en algún almacén del corte / productos críticos evaluados × 100. Se cuentan una vez; se exige política para cada par crítico evaluado. Es una medición al último corte, cuya fecha/lote se devuelve; generar el corte al cierre de la evaluación.
- **Rotación**: unidades SALIDA en el período / stock promedio ponderado por tiempo. Transferencias y ajustes no son consumo comercial. Devuelve consumo, stockPromedio y rotacion por par con movimientos anteriores a hasta. Promedio cero devuelve rotacion=null.
- Sin población/conteos o sin corte pertinente: numerador=0, denominador=0, valor=null y sinDatos=true. No se interpreta ausencia de datos como un 0 % medido. I06/I08/I09 devuelven 400 indicando que no están implementados.

El escenario demo inicial permite contrastar I05=100 % (2 de 2, por almacenes con cero), I07=50 % (1 de 2), I10=50 % (1 de 2). Seleccionar desde antes de cargar la demo, hasta después del corte, y filtrar el SKU para aislar resultados cuando haya otros datos.

## Pruebas y demostración

`./mvnw test` ejecuta pruebas de catálogo, contratos HTTP y la integración real de JPA/Flyway/Spring Security con H2 en modo PostgreSQL. El motor H2 es solo una dependencia de test; la aplicación utiliza PostgreSQL.

Para repetir las mismas pruebas contra PostgreSQL, definir TEST_DB_URL, TEST_DB_USER y TEST_DB_PASSWORD hacia una base desechable distinta de producción y ejecutar `./mvnw test`. Las pruebas crean usuarios y productos de prueba y no limpian una base compartida. No ejecutar contra datos reales. La prueba de demo usa una segunda base: definir también DEMO_TEST_DB_URL para comprobarla en PostgreSQL; por defecto usa H2. Consulte [el resultado de validación](APF2-VALIDACION.md).

Las pruebas incluyen creación concurrente de saldo, salidas concurrentes, transferencias opuestas, reversión provocada al guardar la segunda mitad, sobreflujo numérico, JPQL/paginación, indicadores conocidos, conteos obsoletos, login real, token ausente/inválido/expirado y permisos. Mockito usa mock-maker-subclass para que las pruebas no dependan de adjuntar un agente dinámico a la JVM.

Usar `StockGuard-APF2.postman_collection.json` con baseUrl, adminUsername, adminPassword y demoPassword. Las credenciales se definen en variables locales de Postman. Ejecutar con el perfil demo: obtiene tokens, demuestra lecturas permitidas, 401/403, persistencia de una entrada y consulta de indicadores. Los indicadores comparables dependen del instante del corte; repetir POST /cortes-stock tras cambios antes de evaluar el cierre.

Para evidenciar persistencia entre reinicios: guardar IDs del producto/almacén, registrar una entrada con token de almacén, consultar saldo, detener y volver a iniciar la API sin modificar la BD y volver a consultar el mismo par. Los saldos y movimientos permanecen; el perfil demo no suma otra vez los datos iniciales.
