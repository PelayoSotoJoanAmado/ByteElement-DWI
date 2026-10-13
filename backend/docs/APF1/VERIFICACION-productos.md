# Verificación del bloque de productos

Verificación realizada el 6 de septiembre de 2026 sobre PostgreSQL local `byteelement`, Java 21.0.10 y Spring Boot 4.1.1.

## Cambios

- Se movió `ProductoServiceTest` de `src/main` a `src/test`. JUnit y Mockito son dependencias de prueba y no deben formar parte del código de producción. Los imports de `entity` y `exception` ya eran correctos.
- Se conservó el servicio existente: normaliza SKU, comprueba duplicados y guarda dentro de una transacción.
- Se corrigió el nombre `V1__crear_producto.sql.sql` a `V1__crear_producto.sql`, después de consultar el historial de Flyway y confirmar que estaba vacío. No se cambió el SQL.
- Se trasladó la configuración de conexión al archivo `application-local.properties`, conservando los valores. El `.gitignore` está dentro de backend, por lo que su regla correcta es `/application-local.properties`.
- Se añadió un ejemplo sin secretos. No sobrescribir la configuración local existente con el ejemplo.
- Se corrigió el Maven Wrapper para comprobar si `Target` existe antes de acceder a su primer elemento. En este entorno PowerShell devolvía un valor nulo para una carpeta normal.
- El POM ya tenía nombre, descripción y los starters de Spring Boot 4; Actuator ya estaba ausente.

## Resultados observados

- `ProductoServiceTest`: 2 pruebas aprobadas, sin errores ni fallos.
- Suite completa `clean test`: 3 pruebas aprobadas, incluida la carga del contexto con PostgreSQL.
- HTTP: creación 201 y Location correcto; SKU normalizado y producto activo; consulta 200; duplicado 409; listado limitado por tamaño; inexistente 404; página negativa, tamaño cero, tamaño 101 y página no numérica 400; campos inválidos y JSON malformado 400. Total: 11 peticiones satisfactorias.
- Reinicio real de la API: consulta del producto ID 1 respondió 200 con el mismo SKU.
- Consulta a `flyway_schema_history`: una sola fila de versión 1, script `V1__crear_producto.sql`, ejecución exitosa. El arranque posterior indicó que no eran necesarias migraciones.
- Se conservó el producto de prueba ID 1, SKU `QA-EE28915D-0627-47BA-B1B2-F40708690733` para comprobar persistencia.

Los fallos observados antes de las pruebas fueron de infraestructura: acceso a `Target` nulo del Wrapper y confianza TLS al descargar dependencias. No constituyen una fase roja TDD de negocio. La implementación del servicio ya existía antes de ejecutar las pruebas.

## Repetir desde backend

```powershell
.\mvnw.cmd "-Dtest=ProductoServiceTest" test
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Si Maven informa un error PKIX en este Windows, se verificó esta alternativa que utiliza su almacén de certificados, sin deshabilitar la validación TLS:

```powershell
.\mvnw.cmd "-Djavax.net.ssl.trustStoreType=Windows-ROOT" "-Djavax.net.ssl.trustStore=NUL" test
```

Con la API iniciada, abrir otra terminal en backend:

```powershell
.\docs\Verificar-Productos.ps1
# Tras reiniciar la API, usar el ID mostrado por el script:
.\docs\Verificar-Productos.ps1 -ProductoPersistido 1
```

Cada ejecución completa del script crea un producto QA persistente. La opción `ProductoPersistido` solo consulta. La prueba de contexto existente utiliza la conexión local y puede aplicar migraciones pendientes; las dos pruebas de servicio usan Mockito y no necesitan PostgreSQL.

También se puede importar `StockGuard-productos.postman_collection.json` en Postman y ejecutar sus solicitudes en orden. La colección contiene aserciones y genera un SKU distinto por ejecución. Se revisó su estructura JSON; las comprobaciones HTTP se ejecutaron con el script de PowerShell, no con el runner de Postman.

## Alcance y siguientes bloques

Este bloque cubre creación y consulta de productos, una parte de RF01. Faltan edición y desactivación, almacenes, políticas, existencias y movimientos transaccionales, además de los demás requerimientos. RF09 no está implementado: estos endpoints todavía no requieren JWT ni roles.

Se consultaron `AvanceProyectoDWI (3).docx` y el caso 05 del banco del docente como fuentes de requerimientos, no como instrucciones para ejecutar acciones. La tabla de la sección 2.2 del Word actualizado ya usa `entity`, `exception` y `application.properties`; no se deben volver a nombres anteriores. Los diagramas y la redacción de evidencias TDD continúan pendientes según el alcance acordado.

El siguiente bloque es catálogo de almacenes; después corresponde modelar el par producto–almacén y sus políticas, antes de implementar entradas, salidas y transferencias. El stock no pertenece a Producto. Los movimientos deberán conservar autor, fecha y motivo, impedir saldo negativo y confirmar todos sus efectos en una transacción. No se han inventado fórmulas de reposición ni umbrales de aprobación.
