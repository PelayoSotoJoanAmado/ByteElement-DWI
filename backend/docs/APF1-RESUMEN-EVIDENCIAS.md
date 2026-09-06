# StockGuard MYPE - Evidencias y Alcance APF1

**Proyecto:** StockGuard MYPE por ByteElement: Sistema de Gestión de Inventarios  
**Curso:** Desarrollo Web Integrado (UTP) - APF1 (Avance de Proyecto Final 1)  
**Fecha:** 6 de septiembre de 2026  
**Entorno de ejecución:** Java 21.0.10, Spring Boot 4.1.1, PostgreSQL local 18.1, Maven Wrapper.

---

## 1. Alcance oficial de APF1 según la ruta del docente

De acuerdo con el documento docente (*Banco de 10 Casos*, Caso 05: StockGuard MYPE), los entregables de **APF1** comprenden:
1. **Problema, matrices e indicadores:** Planteamiento, variables, línea base y trazabilidad (documentado en el Word del proyecto).
2. **Actores, reglas y alcance:** Catálogo base, almacenes y reglas críticas de inventario (cero stock negativo y transferencias atómicas).
3. **Arquitectura inicial:** Monolito organizado por capas (`controller`, `dto`, `service`, `entity`, `repository`, `exception`, `seguridad`), inyección por constructor y DTOs inmutables (`record`).
4. **API REST con endpoints base y TDD en reglas críticas:** Endpoints funcionales demostrables con pruebas automatizadas (Fase Roja $\rightarrow$ Fase Verde) y colección de pruebas.

> **Nota de alineación pedagógica:** Persistencia relacional avanzada en BD, consultas JPQL complejas, roles/permisos y JWT corresponden a **APF2**; el cliente Angular e integración visual corresponden a **APF3**. Para APF1 se mantuvieron los productos persistidos con PostgreSQL/Flyway y el flujo operativo de inventario con gestión de existencias en memoria para una demostración ágil e inmediata de las reglas de negocio.

---

## 2. Endpoints Implementados y Verificados (`/api/v1`)

| Método | Ruta | Función / Regla asociada | Códigos HTTP |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/productos` | Registrar producto (normaliza SKU en mayúsculas y trim) | 201 (Location), 400, 409 |
| **GET** | `/api/v1/productos` | Listar catálogo de productos con paginación (`pagina`, `tamanio`) | 200, 400 |
| **GET** | `/api/v1/productos/{id}` | Consultar detalle de un producto por su identificador | 200, 404 |
| **GET** | `/api/v1/almacenes` | Listar los 3 almacenes (A01 Tienda, A02 Depósito, A03 Despacho) | 200 |
| **GET** | `/api/v1/almacenes/{id}` | Consultar detalle de un almacén individual por su ID | 200, 404 |
| **POST** | `/api/v1/movimientos` | Registrar entrada o salida con autor, fecha, motivo y cálculo de saldo | 201 (Location), 400, 404, **409** |
| **GET** | `/api/v1/movimientos` | Listar historial y auditoría de movimientos (filtro opcional por producto/almacén) | 200 |
| **GET** | `/api/v1/movimientos/{id}` | Consultar detalle y trazabilidad de un movimiento registrado | 200, 404 |
| **POST** | `/api/v1/transferencias` | Transferencia atómica entre almacenes (descuenta origen y aumenta destino) | 201 (Location), 400, 404, **409** |
| **GET** | `/api/v1/existencias` | Consultar saldo actual del par (`productoId`, `almacenId`) | 200, 404 |

---

## 3. Evidencia TDD en Reglas Críticas

### Regla 1: Unicidad y normalización de SKU (`ProductoServiceTest`)
- **Regla:** El SKU se limpia de espacios en los extremos y se convierte a mayúsculas. No se permite duplicidad.
- **Resultado:** 2 pruebas unitarias aprobadas.
  - `crearProductoNormalizaSkuYGuarda()` $\rightarrow$ OK
  - `crearProductoConSkuDuplicadoLanzaExcepcion()` $\rightarrow$ OK

### Regla 2: Cero stock negativo y trazabilidad (`InventarioServiceTest`)
- **Regla:** Una salida jamás puede dejar el saldo por debajo de 0. Si la cantidad solicitada supera la existencia disponible, se rechaza con `StockInsuficienteException` (HTTP 409 Conflict), sin modificar el saldo ni generar movimiento fantasma.
- **Ciclo TDD:**
  1. **Fase Roja:** Se definieron las pruebas unitarias con el método arrojando `UnsupportedOperationException`.
  2. **Fase Verde:** Se codificó la lógica en `InventarioService` calculando existencias atómicamente por par `(productoId, almacenId)`.
  3. **Resultado:** 12 pruebas unitarias aprobadas al 100%.

### Regla 3: Transferencias atómicas entre almacenes (`InventarioServiceTest` y `InventarioControllerTest`)
- **Regla docente:** *"Una transferencia debe descontar y aumentar existencias de forma atómica"*.
- **Comprobación:**
  - Si el almacén de origen tiene saldo suficiente, descuenta del origen y añade al destino, registrando dos movimientos vinculados en el mismo instante.
  - Si el almacén origen no tiene suficiente stock, la operación se cancela por completo (`409 Conflict`) y **ninguno** de los dos almacenes ve afectado su saldo.
  - Si el origen y destino son el mismo almacén, se rechaza de inmediato con `400 Bad Request`.

### Regla 4: Capa Web y Códigos de Estado REST (`InventarioControllerTest`)
- 12 pruebas con `MockMvc` verificando los códigos HTTP 200, 201 con cabecera `Location`, 400 ante datos inválidos, 404 ante recursos inexistentes y 409 ante conflicto de negocio.

---

## 4. Resumen Total de Pruebas Automatizadas

Ejecución verificada con `.\mvnw.cmd test`:
- `StockguardApplicationTests`: 1 prueba (arranque de contexto y configuración)
- `ProductoServiceTest`: 2 pruebas
- `InventarioServiceTest`: 12 pruebas
- `InventarioControllerTest`: 12 pruebas
- **Total:** **27 pruebas ejecutadas, 0 fallos, 0 errores (BUILD SUCCESS)**.

---

## 5. Preguntas Frecuentes de Arquitectura y Configuración

### ¿Por qué se usa `record` en los DTOs como `AlmacenResponse`?
- En Java moderno (Java 16 en adelante, y en nuestro Java 21), un `record` es la forma estándar y recomendada para crear **DTOs (Data Transfer Objects)**.
- Un `record` no tiene relación con si los datos vienen o no de una base de datos: es simplemente una clase inmutable diseñada para transportar datos (genera automáticamente constructor, getters como `.id()`, `.equals()`, `.hashCode()` y `.toString()`).
- Las entidades de base de datos JPA (`@Entity`) siguen siendo clases normales (`class Producto`) porque Hibernate requiere mutabilidad y proxies; los DTOs que viajan por la red como JSON son `record` por seguridad e inmutabilidad.

### ¿Cómo funciona la conexión a base de datos y la creación de tablas?
- En `application.properties` está configurado: `spring.jpa.hibernate.ddl-auto=validate`.
- Esto significa que **Hibernate NO crea tablas automáticamente**. Hibernate solo valida que la tabla exista en PostgreSQL.
- **¿Quién crea las tablas? Flyway.** Cuando la aplicación arranca, Flyway busca los scripts SQL en `src/main/resources/db/migration/` (como `V1__crear_producto.sql`) y los ejecuta en orden en la base de datos `byteelement`.

### ¿Cómo comparto el proyecto con mis compañeros sin exponer contraseñas?
- El archivo `.gitignore` del backend tiene la regla:
  ```gitignore
  /application-local.properties
  ```
- Por tanto, tu archivo real con contraseñas **nunca se subirá a GitHub ni se filtrará**.
- En el repositorio existe el archivo de plantilla:
  `backend/application-local.properties.example`
- Tus compañeros, al clonar el repositorio, solo deben crear su propio archivo `application-local.properties` al lado del `pom.xml` con su contraseña local de PostgreSQL.

---

## 6. Instrucciones para la Demostración Local

### A. Ejecutar suite de pruebas completa (27 tests):
```powershell
.\mvnw.cmd test
```

### B. Iniciar la aplicación backend:
```powershell
.\mvnw.cmd spring-boot:run
```

### C. Ejecutar el script automatizado de verificación APF1 (en otra terminal de PowerShell):
```powershell
.\docs\Verificar-APF1.ps1
```

### D. Ejecución mediante Postman:
Importar en Postman el archivo:
`backend/docs/StockGuard-APF1.postman_collection.json`  
Ejecutar la colección completa (las 14 peticiones se ejecutan con aserciones automáticas en milisegundos).
