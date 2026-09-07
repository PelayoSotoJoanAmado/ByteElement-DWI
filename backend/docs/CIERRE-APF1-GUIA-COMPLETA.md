# StockGuard MYPE: guía completa de cierre de APF1

Esta guía explica qué contiene el backend, cómo funciona, qué requisitos demuestra, cómo instalarlo en otra computadora, de dónde salen sus datos y cómo presentarlo en clase. Describe el estado revisado el **7 de septiembre de 2026**. No reemplaza los requisitos del Word ni certifica la aprobación académica del avance.

## Lectura recomendada

1. Para preparar otra computadora: leer «Instalación para cada integrante».
2. Para entender los datos: leer «Cuándo se insertaron datos».
3. Para estudiar el código: leer «Conceptos y decisiones» y la matriz requisito → código → prueba.
4. Para exponer: seguir «Demostración en clase» y el guion de preguntas al final.
5. Para planificar el siguiente avance: consultar los pendientes; no interpretar un endpoint existente como un requisito totalmente aceptado.

Archivos relacionados:

- [Matriz de endpoints y evidencias](APF1-RESUMEN-EVIDENCIAS.md).
- [Migración de productos V1](../src/main/resources/db/migration/V1__crear_producto.sql).
- [Migración de almacenes y políticas V2](../src/main/resources/db/migration/V2__crear_almacen_y_politica_stock.sql).
- [Configuración de ejemplo](../application-local.properties.example).
- [Demostración RF01 en PowerShell](Verificar-RF01.ps1).
- [Demostración de inventario en PowerShell](Verificar-APF1.ps1).
- [Colección Postman RF01](StockGuard-RF01.postman_collection.json).
- [Colección Postman inventario APF1](StockGuard-APF1.postman_collection.json).

## 1. Qué es el proyecto y qué se entrega ahora

StockGuard MYPE por ByteElement es un sistema académico de inventarios para componentes y periféricos de computadoras. El caso plantea tres almacenes, problemas de stock, diferencias entre saldos y conteos, urgencias y solicitudes de compra duplicadas.

La solución completa del curso abarcará catálogo, inventario, compras, indicadores, seguridad e integración. Este cierre corresponde al **backend inicial de APF1**: API REST, reglas demostrables, pruebas y trazabilidad con el informe.

La ampliación actual se concentró en RF01. También existe una demostración parcial de RF02. El proyecto todavía no tiene frontend, JWT, compras ni indicadores calculados. Tener la fórmula de un indicador en el informe no significa que ya exista su cálculo en el código.

El documento docente distribuye el trabajo por avances: API/TDD inicial en APF1; persistencia, JPQL, transacciones y seguridad en APF2; Angular e integración en APF3. Ya se adelantó persistencia del catálogo para respaldar los endpoints solicitados.

## 2. Cuándo se insertaron datos en la base

### 2.1 Cronología de lo ejecutado

| Momento | Acción observada | Efecto |
|---|---|---|
| Revisión inicial del bloque productos, 6 de septiembre | Se consultó flyway_schema_history y estaba vacío | Todavía no había migraciones aplicadas |
| Ejecución de la prueba de contexto del bloque productos | Flyway aplicó V1 | Creó la tabla producto; V1 no inserta productos |
| Pruebas HTTP de productos | POST /api/v1/productos | Se insertaron productos QA usando ProductoService y ProductoRepository |
| Ampliación de RF01, 7 de septiembre | Se ejecutó la suite completa; StockguardApplicationTests levantó el contexto | Flyway aplicó V2, creó almacen y politica_stock e insertó A01, A02 y A03 |
| Pruebas HTTP RF01 | Se ejecutó Verificar-RF01.ps1 | Creó otro producto, un almacén QA y una política; luego desactivó producto y almacén |
| Reinicio de la API | Flyway validó el historial; se consultaron recursos por HTTP | Las migraciones no se repitieron; catálogo y política permanecieron guardados |

En la revisión quedó como evidencia local la política ID 1 para producto ID 3 y almacén ID 4, con mínimo 2/máximo 20 y ambos registros inactivos. **Esos IDs no son valores obligatorios del proyecto ni deben copiarse como si fueran universales.** Dependen de lo insertado en cada base.

### 2.2 Dos formas distintas de insertar datos

**Datos iniciales compartidos:** V2 contiene este SQL:

```sql
INSERT INTO almacen (codigo, nombre) VALUES
    ('A01', 'Tienda principal'),
    ('A02', 'Depósito'),
    ('A03', 'Despacho');
```

Son los almacenes del escenario simulado del Word. Todos reciben esa definición si reciben el archivo V2. Flyway la ejecuta una vez por base al aplicar esa versión.

**Datos creados durante el uso:** un POST de productos o almacenes y un PUT de política llegan a un servicio, que guarda usando un repositorio JPA. Los scripts de demostración realizan estas peticiones y generan datos QA. No existe una migración que copie todos los productos de mi prueba a las computadoras del equipo.

### 2.3 Qué recibe alguien que copia o clona el repositorio

| Elemento | ¿Viaja con el repositorio? | ¿Cómo aparece en su entorno? |
|---|---|---|
| Código Java, POM y Maven Wrapper | Sí, si están incluidos en los archivos compartidos/commit | Al copiar o clonar |
| SQL de V1 y V2 | Sí, si se incluyen | Flyway los lee al arrancar |
| Tablas de catálogo | Viaja su definición, no la base local | Flyway las crea en la base de cada integrante |
| A01, A02 y A03 | Viaja el INSERT de V2 | Se insertan al aplicar V2 |
| Productos QA de esta computadora | No | Cada integrante puede crearlos por API o scripts |
| Política QA de esta computadora | No | Se crea mediante el endpoint o script RF01 |
| Contraseña local | No debe compartirse | Cada integrante configura su propia credencial |
| Saldos y movimientos actuales | No | Solo viven en la memoria de la API que los creó |
| Reportes de Maven en target | Normalmente no | Se generan al ejecutar pruebas |
| Logs históricos de docs | Solo si se incluyen | Son evidencia pasada, no datos para poblar la base |

**Copiar el repositorio no copia PostgreSQL.** El repositorio contiene instrucciones para reconstruir el esquema y los datos iniciales, no una réplica de todas las filas de tu equipo.

El resultado inicial esperado en una base nueva, después de arrancar y antes de usar los scripts, es:

- Tabla producto: vacía.
- Tabla almacen: tres filas iniciales activas.
- Tabla politica_stock: vacía.
- flyway_schema_history: registros exitosos de V1 y V2.
- Movimientos y existencias: mapas vacíos en el proceso Java.

### 2.4 Cómo obtener datos para todos

Para compartir una **demostración equivalente**, basta con incluir las migraciones y los scripts. Cada integrante prepara su base, arranca la API y ejecuta:

```powershell
.\docs\Verificar-RF01.ps1
.\docs\Verificar-APF1.ps1
```

Los scripts generan códigos distintos para evitar duplicados. Las reglas y los resultados esperados son iguales aunque cambien los IDs y SKU. El script RF01 deja sus registros QA de catálogo desactivados; el de inventario crea su propio producto para demostrar movimientos.

Si en otra etapa se necesita una **copia exacta de todas las filas**, habría que preparar un respaldo/restauración de PostgreSQL o un conjunto fijo de datos de prueba. Este cierre no añade un respaldo ni una nueva carga fija de productos. No hace falta copiar datos particulares para ejecutar las pruebas actuales.

### 2.5 Qué pasa en cada reinicio

- Producto, Almacen y PoliticaStock permanecen en PostgreSQL.
- La API vuelve a cargar su contexto y Flyway valida el esquema.
- Las migraciones ya aplicadas no vuelven a ejecutarse normalmente.
- Movimientos y saldos se vacían porque sus mapas se crean de nuevo.
- Una consulta de existencia puede devolver cero para un producto/almacén que sí existen, porque aún no se registraron movimientos en la nueva ejecución.
- No deben presentarse los mapas como historial duradero del sistema.

## 3. Instalación para cada integrante

### 3.1 Requisitos del entorno

El proyecto está configurado para **Java 21**, **Spring Boot 4.1.1** y Maven Wrapper. La revisión se ejecutó con Java 21.0.10 y PostgreSQL 18.1, en Windows/PowerShell.

El equipo necesita:

- Un JDK 21 para compilar.
- PostgreSQL instalado y su servicio iniciado.
- El proyecto completo, incluidos archivos ocultos como .mvn.
- Acceso inicial a las dependencias Maven o una caché que ya las contenga.
- VS Code u otro editor; Postman es opcional.

Verificar Java:

```powershell
java -version
```

No es necesario instalar Maven globalmente: se usa `mvnw.cmd`. En este repositorio el Wrapper contiene un ajuste para manejar una propiedad Target vacía en PowerShell. Mantenerlo junto a `.mvn/wrapper/maven-wrapper.properties`.

### 3.2 Crear base y usuario solo si no existen

Este paso es **para una instalación nueva**, no para repetirlo sobre la base que ya funciona. Abrir psql o el Query Tool de pgAdmin con una cuenta administradora y crear la cuenta técnica:

```sql
CREATE ROLE stockguard_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
```

Asignarle una contraseña propia mediante la interfaz de pgAdmin o, dentro de psql, con:

```text
\password stockguard_app
```

La contraseña se introduce localmente; no debe guardarse en el repositorio.

Crear la base en una operación independiente, fuera de un bloque de transacción:

```sql
CREATE DATABASE byteelement;
GRANT CONNECT ON DATABASE byteelement TO stockguard_app;
```

Después cambiar la conexión a la base **byteelement**, todavía como administrador, y ejecutar:

```sql
GRANT USAGE, CREATE ON SCHEMA public TO stockguard_app;
```

En psql el cambio de base se hace con `\connect byteelement`. En pgAdmin abrir el Query Tool de esa base.

La aplicación usa la misma cuenta técnica para Flyway y JPA en este entorno local. No necesita superusuario ni permiso para crear roles/bases. Si base y cuenta ya existen, comprobar su configuración; no recrearlas ni cambiar contraseñas para seguir esta guía.

Flyway crea las tablas **dentro** de una base existente. No crea por sí solo el servidor PostgreSQL, el usuario ni la base byteelement.

### 3.3 Preparar la configuración local

Abrir una terminal en `backend`, donde se encuentra `pom.xml`. Crear la configuración solo si falta:

```powershell
if (!(Test-Path -LiteralPath '.\application-local.properties')) {
    Copy-Item -LiteralPath '.\application-local.properties.example' -Destination '.\application-local.properties'
}
```

Editar el archivo local con los valores de esa computadora:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/byteelement
spring.datasource.username=stockguard_app
spring.datasource.password=TU_CONTRASENA_LOCAL
```

El texto TU_CONTRASENA_LOCAL es un marcador: reemplazarlo localmente. No compartir ese archivo. El .gitignore del backend contiene `/application-local.properties`.

La configuración compartida importa el archivo usando:

```properties
spring.config.import=optional:file:./application-local.properties
```

La ruta depende del directorio desde donde arranca el proceso. **Por eso los comandos se ejecutan desde backend.** El prefijo optional permite que el archivo no exista, pero no aporta una conexión de base de datos por sí mismo.

### 3.4 Primer arranque

```powershell
.\mvnw.cmd spring-boot:run
```

En la primera ejecución sobre base vacía, Flyway aplica V1 y V2. Después Hibernate valida las tablas y la API escucha normalmente en `http://localhost:8080`.

En otra terminal:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/v1/almacenes'
```

Deben aparecer los tres almacenes iniciales; podrían aparecer más si ya se usó el sistema. Para detener la API en su terminal, usar Ctrl+C. Si se inicia desde el editor, configurar backend como directorio de trabajo.

### 3.5 Verificar el esquema en pgAdmin

Conectado a byteelement, estas consultas son de lectura:

```sql
SELECT version, script, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT id, codigo, nombre, activo
FROM almacen
ORDER BY id;

SELECT id, sku, categoria, marca, modelo, activo
FROM producto
ORDER BY id;

SELECT id, producto_id, almacen_id, minimo, maximo
FROM politica_stock
ORDER BY id;
```

No crear manualmente tablas que Flyway debe crear. No modificar V1/V2 después de aplicarlas ni borrar el historial para ocultar un error de checksum. Las modificaciones posteriores del esquema se realizan con una nueva migración revisada.

## 4. Conceptos y decisiones del código

### 4.1 Qué es un endpoint

Es una operación HTTP identificada por método y ruta. GET /productos y POST /productos son endpoints diferentes aunque compartan ruta.

- GET consulta.
- POST crea un recurso o registra una operación.
- PUT sustituye los datos editables enviados; la política usa PUT para establecer los límites del par.
- PATCH de desactivación cambia únicamente ese estado.
- No se expuso DELETE de catálogo: la baja lógica conserva las filas.

Un controlador recibe la petición; el servicio decide las reglas; un repositorio accede a la base. La respuesta JSON usa DTOs.

### 4.2 Qué significan las anotaciones

| Elemento | Uso en este proyecto |
|---|---|
| @SpringBootApplication | Punto de entrada y configuración de la aplicación |
| @RestController | Clase que atiende HTTP y devuelve cuerpos como JSON |
| @RequestMapping / @GetMapping / @PostMapping | Asociación de rutas y métodos HTTP |
| @RequestBody | Convertir el JSON recibido al DTO |
| @PathVariable | Leer un ID de la ruta |
| @RequestParam | Leer parámetros como pagina, tamanio o almacenId |
| @Valid | Ejecutar las validaciones del DTO recibido |
| @NotBlank, @NotNull, @Size, @Positive | Restricciones de entrada, según el campo |
| @Service | Componente que contiene operaciones y reglas |
| @Transactional | Delimitar acceso transaccional a la base en los servicios de catálogo |
| @Entity | Mapear una clase como entidad persistente JPA |
| @Table / @Column | Relacionar entidad/campos con tabla/columnas |
| @Id / @GeneratedValue | Identificador cuya generación se delega según la estrategia configurada |
| @ManyToOne / @JoinColumn | Relación de política hacia producto o almacén y su clave foránea |
| @RestControllerAdvice / @ExceptionHandler | Convertir excepciones en respuestas de error |
| @Profile | Limitar la disponibilidad de un componente a determinados perfiles |

InventarioService está disponible con el perfil predeterminado o apf1. No hay un ambiente de producción completo configurado: activar otro perfil arbitrariamente puede dejar componentes requeridos sin crear.

### 4.3 Entidad, DTO, record y enum

**Entidad JPA:** representa una fila persistente. Actualmente hay tres:

| Entidad | Tabla | Datos principales |
|---|---|---|
| Producto | producto | id, sku, categoria, marca, modelo, especificacion, critico, activo |
| Almacen | almacen | id, codigo, nombre, activo |
| PoliticaStock | politica_stock | id, producto_id, almacen_id, minimo, maximo |

**DTO:** define qué recibe o devuelve una operación. CrearProductoRequest sirve para crear y editar; ProductoResponse es la salida. Lo mismo ocurre con GuardarAlmacenRequest, AlmacenResponse y los DTOs de política.

**record:** forma compacta de declarar un portador de datos. Genera constructor, métodos de acceso y operaciones de comparación/representación. No significa que exista una tabla ni añade seguridad automáticamente. Los records actuales contienen valores que se transportan entre capas.

**enum:** conjunto de valores permitidos en código. TipoMovimiento enumera ENTRADA, SALIDA, TRANSFERENCIA_ENTRADA y TRANSFERENCIA_SALIDA. POST /movimientos admite ENTRADA/SALIDA; los otros tipos se generan mediante el endpoint de transferencias.

**Movimiento:** está en la carpeta entity como modelo del negocio, pero no lleva @Entity. Sigue siendo un record en memoria. La carpeta no le da persistencia por sí sola.

### 4.4 Relación producto–almacén–política

Un producto puede tener una política en A01 y otra en A02. Un almacén puede tener políticas para muchos productos.

PoliticaStock tiene referencias a Producto y Almacen. En PostgreSQL son claves foráneas; el par tiene una restricción UNIQUE. La API conserva una política **actual** por par y su PUT modifica sus límites. Todavía no conserva versiones anteriores para calcular políticas vigentes en fechas pasadas.

El saldo no es una columna de Producto: el mismo SKU puede tener 5 unidades en A01 y 20 en A02. Actualmente el saldo se almacena con una clave del tipo `productoId:almacenId` dentro de un mapa.

Los límites no son el saldo. Cambiar máximo a 10 no elimina unidades ni obliga por sí solo a que la existencia sea menor que 10. El máximo servirá para alertas y reposición posteriores; no se implementaron esas fórmulas.

### 4.5 Qué hace cada servicio

- **ProductoService:** normaliza SKU, valida duplicados, crea, edita, consulta y desactiva productos. Al editar, excluye el ID propio en la búsqueda de duplicados.
- **AlmacenService:** crea, consulta, edita y desactiva almacenes persistidos; normaliza su código y comprueba unicidad.
- **PoliticaStockService:** verifica límites y referencias, requiere catálogo activo para guardar, consulta y establece la política actual del par.
- **InventarioService:** valida catálogo, calcula saldos, registra movimientos y transferencias en mapas. Sus lecturas y escrituras relevantes están sincronizadas dentro de una instancia.

Las dependencias se reciben por constructor. Spring crea los componentes y los conecta; en las pruebas se pueden reemplazar dependencias por mocks.

### 4.6 Validación y errores

Las validaciones ocurren en distintos niveles:

1. El DTO rechaza campos ausentes, vacíos o fuera de tamaño.
2. El servicio comprueba reglas como SKU duplicado, máximo mayor que mínimo o saldo insuficiente.
3. La base protege el catálogo con NOT NULL, UNIQUE, CHECK y claves foráneas.

La comprobación previa de duplicados permite una respuesta comprensible; la restricción UNIQUE sigue siendo necesaria si dos peticiones compiten.

ProblemDetail estructura errores de negocio, por ejemplo con status y detail. No debe suponerse que todos los errores posibles tienen exactamente los mismos campos adicionales.

`Math.addExact` detecta una suma que excede la capacidad del número long. Se rechaza antes de cambiar los saldos para evitar que un desbordamiento termine produciendo cantidades negativas.

### 4.7 Transacción no es lo mismo que synchronized

@Transactional coordina operaciones contra la base en los servicios de catálogo. synchronized hace que ciertos métodos de una misma instancia Java no se ejecuten simultáneamente.

El inventario actual usa synchronized y mapas. Esto protege el cálculo dentro de esa instancia durante la demostración; **no equivale a una transferencia transaccional persistida en PostgreSQL**, ni asegura recuperación ante apagado o coordinación de varias instancias.

La respuesta de transferencia incluye los IDs de sus dos movimientos. No hay todavía una entidad Transferencia persistida que mantenga esa relación histórica.

### 4.8 Por qué se desactiva

Cambiar activo a false conserva el ID y las referencias. Se puede seguir consultando el registro. La API rechaza nuevas operaciones de inventario con catálogo inactivo y no permite guardar políticas sobre esos registros.

Editar un producto/almacén no lo reactiva: su estado se conserva. No se implementó endpoint de reactivación. Tampoco se prohíbe consultar una política existente porque el catálogo se haya desactivado.

## 5. Referencia funcional y operativa completa

Las siguientes secciones reúnen la matriz actual, los 17 contratos y la evidencia del cierre. Los conteos corresponden al estado revisado; si cambia el código o se añaden pruebas, se deben actualizar.


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


## Ejemplos JSON para estudiar y probar

Usar IDs devueltos por las respuestas, no asumir que siempre serán 1. En Postman seleccionar Body → raw → JSON y enviar Content-Type: application/json.

### Crear o editar producto

POST /api/v1/productos o PUT /api/v1/productos/{id}:

```json
{
  "sku": "RAM-DEMO-01",
  "categoria": "Memorias RAM",
  "marca": "Kingston",
  "modelo": "DDR4",
  "especificacion": "8 GB, 3200 MHz",
  "critico": true
}
```

El SKU se normaliza con trim y mayúsculas. Usar un SKU nuevo para crear otro producto. El POST devuelve 201, el ID y Location; el PUT devuelve 200. activo comienza en true.

### Crear o editar almacén

POST /api/v1/almacenes o PUT /api/v1/almacenes/{id}:

```json
{
  "codigo": "A-DEMO",
  "nombre": "Almacen de demostracion"
}
```

Código y nombre son obligatorios. El código se normaliza y debe ser único. La creación por API no está restringida a los tres datos iniciales del caso.

### Establecer política

PUT /api/v1/productos/{productoId}/almacenes/{almacenId}/politica-stock:

```json
{
  "minimo": 2,
  "maximo": 20
}
```

La ruta identifica el par; no se repiten sus IDs en el cuerpo. mínimo 0 es válido; máximo igual o menor que mínimo es inválido. Se trabaja con unidades enteras.

### Registrar entrada o salida

POST /api/v1/movimientos:

```json
{
  "productoId": 1,
  "almacenId": 1,
  "tipo": "ENTRADA",
  "cantidad": 10,
  "autor": "Operador de demostracion",
  "motivo": "Recepcion inicial"
}
```

Cambiar tipo a SALIDA para retirar unidades. cantidad debe ser positiva; se rechaza retirar más de lo disponible. La fecha la asigna el servidor. autor es texto recibido y no acredita una sesión autenticada.

### Transferir

POST /api/v1/transferencias:

```json
{
  "productoId": 1,
  "origenAlmacenId": 1,
  "destinoAlmacenId": 2,
  "cantidad": 3,
  "autor": "Operador de demostracion",
  "motivo": "Abastecimiento interno"
}
```

Origen y destino deben existir, estar activos y ser distintos. El producto debe estar activo y el origen debe tener saldo. Se generan dos movimientos y se devuelven ambos saldos resultantes.

### Desactivar

PATCH /api/v1/productos/{id}/desactivar o PATCH /api/v1/almacenes/{id}/desactivar no necesitan JSON. Después se puede consultar el mismo ID y comprobar activo=false.

## Qué prueban realmente los tests

- **JUnit** organiza y ejecuta las pruebas. @Test identifica un caso; @BeforeEach prepara su escenario.
- **Mockito** reemplaza dependencias por dobles controlados. Por ejemplo, simula que un repositorio ya conoce un SKU, sin conectarse a PostgreSQL.
- **MockMvc** ejecuta contratos del controlador dentro del proceso de pruebas. No requiere abrir el puerto 8080.
- **StockguardApplicationTests** levanta el contexto real y verifica que los componentes y la conexión local permiten arrancar. No demuestra por sí solo todos los endpoints.
- **Scripts PowerShell** envían peticiones HTTP reales a una API encendida y comprueban respuestas y efectos. Complementan las pruebas unitarias.
- **Postman** permite observar y ejecutar manualmente los contratos o su colección. Las aserciones de las colecciones son instrucciones de verificación, no prueba de que se hayan ejecutado en todos los equipos.

Los mocks de ProductoServiceTest no insertan filas. La prueba de contexto puede aplicar migraciones pendientes, por eso la suite completa sí puede modificar el esquema local. Los scripts HTTP sí crean datos QA.

TDD significa escribir la prueba antes de implementar el comportamiento, observar el fallo, implementar y volver a comprobar. Un error de descarga de Maven no es una fase roja de negocio. Los logs identificados documentan los ciclos realmente observados, no una reconstrucción ficticia de todo el desarrollo.

## Guion breve de sustentación

“Este avance implementa una API REST de StockGuard organizada por capas. Para RF01 tenemos productos, almacenes y políticas de stock. Las tres entidades están persistidas y las tablas se crean con migraciones Flyway.

Voy a mostrar una regla concreta del informe: mínimo mayor o igual a cero y máximo mayor que mínimo. Este endpoint recibe los límites; este servicio valida la condición; la entidad y la base también protegen los valores. Esta prueba demuestra el caso válido y estas pruebas demuestran los rechazos.

También mostramos un flujo inicial de inventario: una entrada aumenta el saldo y una salida excesiva se rechaza sin cambiarlo. En este avance los movimientos están en memoria. La persistencia completa, los roles y JWT quedan pendientes para la siguiente etapa.”

Si preguntan “¿dónde está RF01?”, abrir PoliticaStockController, PoliticaStockService, PoliticaStock y PoliticaStockServiceTest. Para SKU, abrir ProductoController, ProductoService y ProductoServiceTest.

Si preguntan “¿dónde está RF02?”, abrir InventarioController, InventarioService e InventarioServiceTest, y precisar su límite de memoria.

Si preguntan “¿ya se cumplen todos los requisitos?”, mostrar la tabla de estados. La implementación del curso es progresiva; no se presenta una regla o módulo pendiente como terminado.

## Solución de problemas habituales

| Síntoma | Qué comprobar |
|---|---|
| No encuentra pom.xml | La terminal debe estar en backend |
| Compilación requiere otra versión de Java | Verificar java -version y que Maven use un JDK 21 |
| Error PKIX al descargar | Usar la variante con certificados Windows documentada; no desactivar TLS |
| Connection refused a PostgreSQL | Revisar servicio, host y puerto de la configuración local |
| Password authentication failed | Revisar usuario/contraseña propios sin imprimirlos en logs; no recrear la base |
| Permission denied for schema public | Revisar USAGE y CREATE en la base correcta para stockguard_app |
| No carga application-local.properties | Confirmar archivo junto al POM y arranque desde backend |
| Flyway informa checksum distinto | Revisar si se cambió una migración ya aplicada; no ocultarlo borrando historial |
| Puerto 8080 ocupado | Cerrar el proceso de API que se esté usando o elegir otro puerto y ajustar baseUrl |
| Stock vuelve a cero tras reiniciar | Es el comportamiento actual del inventario en memoria |
| SKU/código duplicado al repetir un ejemplo | Usar un identificador nuevo o los scripts que generan códigos QA |
| Movimiento devuelve 409 con saldo suficiente | Revisar si producto o almacén están desactivados |
| Colección falla al saltar pasos | Ejecutar en orden para establecer las variables de IDs |
| Más de tres almacenes en el listado | La API ya permite crearlos; los tres de V2 son datos iniciales |
| Script usa un ID inexistente | Usar el ID devuelto localmente; no copiar IDs de evidencia ajena |

Si la política de ejecución de PowerShell del equipo bloquea scripts, ejecutar la demostración mediante Postman o revisar la política permitida por ese equipo. Esta guía no requiere cambiar una política global del sistema.

## Cómo compartir el cierre con el equipo

Antes de compartir, comprobar que viajan:

- pom.xml, mvnw, mvnw.cmd y .mvn.
- src/main y src/test completos.
- V1 y V2 sin modificaciones respecto a las versiones aplicadas.
- application-local.properties.example.
- Las colecciones y scripts de docs.
- Este documento, la matriz APF1 y el Word que se use como fuente.

No incluir application-local.properties con contraseñas. Si se comparte un ZIP, **.gitignore no filtra automáticamente el contenido del ZIP**: revisar los archivos incluidos. Si se usa Git, los archivos nuevos no rastreados no viajan con un clon hasta incorporarlos al commit y compartir ese commit.

Comprobaciones desde backend:

```powershell
git status --short
git check-ignore application-local.properties
```

No se hizo commit ni push como parte de este documento. No basta con que un archivo exista en tu computadora para que ya esté disponible en el remoto.

La base de cada integrante debe poder reconstruirse a partir de las migraciones. Los registros QA de una computadora no son un requisito para ejecutar el proyecto en otra.

## Cierre y límites de lo entregado

Este documento es una fotografía del estado actual. No se añadieron nuevas reglas de negocio ni se poblaron más tablas al redactarlo. Resume código, migraciones y verificaciones existentes.

La guía de instalación fue contrastada con los archivos del proyecto; en este cierre no se instaló PostgreSQL desde cero en otra computadora. Las verificaciones de aplicación y persistencia corresponden al entorno local descrito.

Quedan pendientes en el Word los diagramas y la incorporación ordenada de la evidencia TDD. También se debe distinguir el modelo completo planificado del catálogo realmente persistido. Las matrices pueden describir el destino del proyecto, pero su columna de implementación/evidencia debe reflejar el estado de cada avance.

No se debe ampliar módulos solo para aumentar el número de endpoints. Cada siguiente bloque debe conservar la relación requisito → dato → operación → regla → prueba y actualizar la documentación correspondiente.

