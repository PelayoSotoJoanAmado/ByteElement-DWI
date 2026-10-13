# StockGuard MYPE - Guía Maestra de Endpoints (RF01 y RF02)

**Proyecto:** StockGuard MYPE por ByteElement: Sistema de Gestión de Inventarios  
**Curso:** Desarrollo Web Integrado (UTP) - APF1  
**Tecnologías:** Java 21, Spring Boot 4.1.1, PostgreSQL 18.1  

Esta guía resume de forma clara y directa todos los endpoints implementados en el backend, su ubicación en el código fuente y cómo responden exactamente a los requerimientos funcionales **RF01** y **RF02** del documento Word del proyecto.

---

## 🧭 Mapa Rápido de Endpoints

| Requisito | Entidad / Flujo | Método | Ruta | Código HTTP | Controlador |
| :---: | :--- | :---: | :--- | :---: | :--- |
| **RF01** | Productos | `POST` | `/api/v1/productos` | 201, 400, 409 | `ProductoController.java` |
| **RF01** | Productos | `PUT` | `/api/v1/productos/{id}` | 200, 400, 404 | `ProductoController.java` |
| **RF01** | Productos | `PATCH` | `/api/v1/productos/{id}/desactivar` | 200, 404 | `ProductoController.java` |
| **RF01** | Productos | `GET` | `/api/v1/productos` | 200, 400 | `ProductoController.java` |
| **RF01** | Productos | `GET` | `/api/v1/productos/{id}` | 200, 404 | `ProductoController.java` |
| **RF01** | Almacenes | `POST` | `/api/v1/almacenes` | 201, 400 | `AlmacenController.java` |
| **RF01** | Almacenes | `PUT` | `/api/v1/almacenes/{id}` | 200, 400, 404 | `AlmacenController.java` |
| **RF01** | Almacenes | `PATCH` | `/api/v1/almacenes/{id}/desactivar` | 200, 404 | `AlmacenController.java` |
| **RF01** | Políticas Stock | `GET` | `/api/v1/productos/{pId}/almacenes/{aId}/politica-stock` | 200, 404 | `PoliticaStockController.java` |
| **RF01** | Políticas Stock | `PUT` | `/api/v1/productos/{pId}/almacenes/{aId}/politica-stock` | 200, 400, 404 | `PoliticaStockController.java` |
| **RF02** | Movimientos | `POST` | `/api/v1/movimientos` | 201, 400, 404, **409** | `InventarioController.java` |
| **RF02** | Movimientos | `GET` | `/api/v1/movimientos` | 200 | `InventarioController.java` |
| **RF02** | Movimientos | `GET` | `/api/v1/movimientos/{id}` | 200, 404 | `InventarioController.java` |
| **RF02** | Transferencias | `POST` | `/api/v1/transferencias` | 201, 400, 404, **409** | `InventarioController.java` |
| **RF02** | Existencias | `GET` | `/api/v1/existencias` | 200, 404 | `InventarioController.java` |
| **RF02** | Consulta Almacén | `GET` | `/api/v1/almacenes` y `/{id}` | 200, 404 | `InventarioController.java` |

---

## 1. RF01: Administrar productos, almacenes y políticas

> **Criterio de aceptación oficial (Word):**  
> *"Crear y editar SKU único, categoría, marca, modelo y especificación. Solo desactivar registros con historial; mínimo ≥ 0 y máximo > mínimo."*

### A. Catálogo de Productos
📁 **Ubicación:** `backend/src/main/java/com/byteelement/stockguard/controller/ProductoController.java`

1. **`POST /api/v1/productos`**
   - **Qué hace:** Crea un nuevo producto validando campos obligatorios.
   - **Regla aplicada:** Normaliza el SKU (convierte a mayúsculas y quita espacios de los bordes con `trim()`). Si el SKU ya existe en la base de datos, arroja `409 Conflict`.
   - **Respuesta exitosa:** `201 Created` con cabecera `Location: /api/v1/productos/{id}`.

2. **`PUT /api/v1/productos/{id}`**
   - **Qué hace:** Edita la información descriptiva del producto (categoría, marca, modelo, especificación y estado crítico).
   - **Respuesta:** `200 OK` con los datos actualizados o `404 Not Found` si el ID no existe.

3. **`PATCH /api/v1/productos/{id}/desactivar`**
   - **Qué hace:** Desactivación lógica (`activo = false`).
   - **Regla aplicada:** No borra el registro de la base de datos (`DELETE`), lo desactiva para preservar la trazabilidad e historial exigidos.

4. **`GET /api/v1/productos?pagina=0&tamanio=20`**
   - **Qué hace:** Lista productos de forma paginada para no saturar la red ni la memoria.
   - **Validación:** Si se envían páginas negativas o tamaños mayores a 100, devuelve `400 Bad Request`.

5. **`GET /api/v1/productos/{id}`**
   - **Qué hace:** Consulta un producto por su ID (`200 OK` o `404 Not Found`).

---

### B. Catálogo de Almacenes
📁 **Ubicación:** `backend/src/main/java/com/byteelement/stockguard/controller/AlmacenController.java`

1. **`POST /api/v1/almacenes`** $\rightarrow$ Registra un almacén (código único ej. `A01`, `A02`, `A03` y nombre).
2. **`PUT /api/v1/almacenes/{id}`** $\rightarrow$ Actualiza el nombre o datos del almacén.
3. **`PATCH /api/v1/almacenes/{id}/desactivar`** $\rightarrow$ Desactiva lógicamente el almacén.

---

### C. Políticas de Stock (Mínimos y Máximos)
📁 **Ubicación:** `backend/src/main/java/com/byteelement/stockguard/controller/PoliticaStockController.java`

1. **`GET /api/v1/productos/{productoId}/almacenes/{almacenId}/politica-stock`**
   - **Qué hace:** Devuelve los umbrales de stock configurados para ese par específico (producto - almacén).

2. **`PUT /api/v1/productos/{productoId}/almacenes/{almacenId}/politica-stock`**
   - **Qué hace:** Configura los niveles de stock de reposición.
   - **Regla crítica del Word aplicada:**
     - **`mínimo >= 0`**
     - **`máximo > mínimo`**
   - Si se envía un máximo menor o igual al mínimo, el servicio rechaza la solicitud de inmediato impidiendo inconsistencias.

---

## 2. RF02: Registrar entradas, salidas y transferencias

> **Criterio de aceptación oficial (Word):**  
> *"Cada operación guarda autor, fecha y motivo; valida disponibilidad y confirma todos sus efectos en una transacción."*

📁 **Ubicación:** `backend/src/main/java/com/byteelement/stockguard/controller/InventarioController.java`  
📁 **Servicio de Reglas:** `backend/src/main/java/com/byteelement/stockguard/service/InventarioService.java`

### 1. Entradas y Salidas de Mercadería
- **`POST /api/v1/movimientos`**
  - **Body requerido:** `productoId`, `almacenId`, `tipo` (`ENTRADA` o `SALIDA`), `cantidad`, `autor`, `motivo`.
  - **Regla de ENTRADA:** Suma las unidades al saldo del almacén y registra el movimiento con `fecha` (`Instant.now()`), `autor`, `motivo` y `saldoResultante`.
  - **Regla Crítica de SALIDA (No saldo negativo):** 
    - Comprueba el saldo disponible en ese almacén.
    - Si `saldo < cantidad`, lanza `StockInsuficienteException` devolviendo **`409 Conflict`**.
    - **Efecto protector:** El saldo queda **intacto** y **no** se genera ningún movimiento fantasma.

### 2. Transferencia Atómica entre Almacenes
- **`POST /api/v1/transferencias`**
  - **Body requerido:** `productoId`, `origenAlmacenId`, `destinoAlmacenId`, `cantidad`, `autor`, `motivo`.
  - **Regla atómica del docente:**
    1. Verifica que `origen != destino` (si son iguales devuelve `400 Bad Request`).
    2. Comprueba que el almacén de origen tenga saldo suficiente para cubrir la cantidad.
    3. Si no alcanza, cancela la operación con **`409 Conflict`** (ningún almacén cambia de saldo).
    4. Si alcanza, en una sola operación sincronizada: descuenta de origen y añade a destino, generando los 2 movimientos de auditoría vinculados.

### 3. Consultas y Auditoría de Inventario
- **`GET /api/v1/existencias?productoId=1&almacenId=1`**
  - Devuelve el saldo actual exacto en unidades enteras.
- **`GET /api/v1/movimientos?productoId=1`**
  - Lista toda la trazabilidad y auditoría cronológica de movimientos registrados para ese producto.
- **`GET /api/v1/movimientos/{id}`**
  - Consulta el comprobante individual de un movimiento por su ID.
- **`GET /api/v1/almacenes` y `GET /api/v1/almacenes/{id}`**
  - Lista y consulta los almacenes habilitados para la operativa.

---

## 3. Códigos de Estado HTTP Utilizados

| Código | Significado | ¿Cuándo se devuelve? |
| :---: | :--- | :--- |
| **`200 OK`** | Operación exitosa | Consultas GET y actualizaciones PUT/PATCH exitosas. |
| **`201 Created`** | Recurso creado | Registro exitoso de productos, movimientos o transferencias (con cabecera `Location`). |
| **`400 Bad Request`** | Petición inválida | Campos vacíos, cantidades negativas o almacén origen igual a destino. |
| **`404 Not Found`** | No encontrado | Cuando el ID del producto, almacén o movimiento no existe. |
| **`409 Conflict`** | Conflicto de regla | **SKU duplicado** o **Stock insuficiente** al intentar una salida o transferencia. |

---

## 4. Cómo Demostrarlo Rápido

1. **Compilar y verificar todas las pruebas automatizadas:**
   ```powershell
   .\mvnw.cmd test
   ```
   *(Pasan las 27 pruebas al 100%).*

2. **Arrancar el backend:**
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. **Ejecutar la verificación completa en PowerShell:**
   ```powershell
   .\docs\Verificar-APF1.ps1
   ```

