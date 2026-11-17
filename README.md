# ByteElement-DWI - StockGuard MYPE

Para clonar y arrancar por primera vez, seguir [GUIA-INICIO.md](GUIA-INICIO.md).  
Para conocer cada función y recorrer el sistema, consultar [GUIA-USO-SISTEMA.md](GUIA-USO-SISTEMA.md).  
Para explicar las fórmulas, la medición y su relación con el informe, consultar [GUIA-INDICADORES-Y-TRAZABILIDAD.md](GUIA-INDICADORES-Y-TRAZABILIDAD.md).

Última corrección: [selectores de almacenes](frontend/docs/SELECTORES-ALMACENES.md), activos por defecto y consulta histórica opcional. 102 pruebas Angular, build y siete comprobaciones reales aprobados.

Revisión integrada local: [flujo, base existente y resultados](backend/docs/APF3/revision-integrada/README.md).  
94 pruebas backend por motor, 102 Angular y 20 grupos de navegador aprobados.  
Node actualizado a 26.13.1: pruebas Angular y build comprobados de nuevo; la base existente se conserva al migrar a V8.

Actualización de cuentas (16/11): [instalación normal de cinco roles](backend/docs/APF3/cuentas/README.md), [contrato detallado para Angular](backend/docs/APF3/cuentas/CONTRATO-FRONTEND.md) y [cambios al informe](backend/docs/APF3/cuentas/CAMBIOS-INFORME.md). Las cuentas nuevas deben cambiar su password antes de operar; Angular ya implementa ese flujo.

Backend Java 21 / Spring Boot / JPA-Hibernate / PostgreSQL para inventario y reposición.  
APF3 completa compras, recepciones, reglas de reposición, I05-I10, evidencias de backend y CORS para Angular.  
La carpeta `frontend/` contiene la base Angular con SCSS, la presentación pública, el inicio de sesión y el panel de administrador protegido de StockGuard.  
Las cinco áreas de rol están integradas; los recorridos locales verificados y sus límites se documentan en la revisión integrada.

- [Estado y traspaso](HANDOVER_ESTADO_PROYECTO.md)
- [Documentación APF3](backend/docs/APF3/README.md)
- [Contrato para Angular](backend/docs/APF3/API-ANGULAR.md)
- [Cambios que deben trasladarse al informe](backend/docs/APF3/CAMBIOS-INFORME.md)
- [Arranque, volumen, backup e InsForge](backend/docs/APF3/OPERACION.md)
- [Colección Postman APF3](backend/docs/APF3/StockGuard-APF3.postman_collection.json)
- [Evidencia de pruebas](backend/docs/APF3/evidencias/resultado.json)

---

## Credenciales de Acceso al Sistema

Para iniciar sesión en la aplicación web (`http://localhost:4200/login`), se dispone de los siguientes usuarios precargados y verificados en la base de datos cloud, estructurados según los roles y permisos de la matriz RBAC:

### Cuentas Principales por Rol (Demostración y Evaluación)

| Rol | Nombre de Usuario | Correo Electrónico | Contraseña | Responsabilidad / Permisos en el Sistema |
| :--- | :--- | :--- | :--- | :--- |
| **ADMINISTRADOR** | `demo-administrador` | `admin@byteelement.pe` | `DemoPassword123!` | Control total: Catálogo de 10k productos, almacenes, gestión de usuarios, auditoría y reportes del sistema. |
| **ALMACEN** | `demo-almacen` | `almacen@byteelement.pe` | `DemoPassword123!` | Registro de entradas, salidas (despachos), transferencias inter-almacén, recepciones de compras y conteos físicos. |
| **COMPRAS** | `demo-compras` | `compras@byteelement.pe` | `DemoPassword123!` | Consulta de alertas de reposición, emisión de solicitudes de compra (con clave de idempotencia) y emisión de órdenes de compra. |
| **SUPERVISOR** | `demo-supervisor` | `supervisor@byteelement.pe` | `DemoPassword123!` | Aprobación/rechazo de solicitudes de compra, definición de políticas de stock, autorización de ajustes de inventario y consulta de indicadores analíticos (I05–I10). |
| **CONSULTA_COMERCIAL** | `demo-consulta_comercial` | `consulta@byteelement.pe` | `DemoPassword123!` | Consulta de solo lectura de catálogo de productos, existencias en almacenes y kardex de movimientos. |

### Cuentas Nominales del Personal (Base de Datos Operativa)

| Rol | Nombre de Usuario | Correo Electrónico | Contraseña | Colaborador / Cargo |
| :--- | :--- | :--- | :--- | :--- |
| **ADMINISTRADOR** | `arojas_admin` | `arojas@byteelement.pe` | `DemoPassword123!` | Alberto Rojas (Administrador de Sistemas) |
| **ALMACEN** | `jquispe_almacen` | `jquispe@byteelement.pe` | `DemoPassword123!` | Juan Quispe (Jefe de Almacén) |
| **COMPRAS** | `mflores_compras` | `mflores@byteelement.pe` | `DemoPassword123!` | María Flores (Analista de Compras) |
| **SUPERVISOR** | `cramirez_supervisor` | `cramirez@byteelement.pe` | `DemoPassword123!` | Carlos Ramírez (Supervisor de Operaciones) |
| **CONSULTA_COMERCIAL** | `lsoto_consulta` | `lsoto@byteelement.pe` | `DemoPassword123!` | Luis Soto (Asesor de Ventas) |

> [!NOTE]
> * **Acceso al sistema:** En la pantalla de inicio de sesión (`/login`), ingrese el **Nombre de Usuario** en el campo `Usuario` y la **Contraseña** correspondiente.
> * **Estado de cuentas:** Todas las cuentas se encuentran activas y listas para operar (`requiereCambioPassword = false`), permitiendo ingresar directamente a sus respectivos paneles de control.
> * **Gestión voluntaria:** Cada usuario puede cambiar su contraseña voluntariamente desde la opción **Mi cuenta** (`/admin/mi-cuenta` o `/almacen/mi-cuenta`).

---

## Ejecución del Backend

Desde la carpeta `backend/`:

```powershell
./mvnw.cmd test
```

Para arrancar el servidor en desarrollo local:
```powershell
./mvnw.cmd spring-boot:run
```

* El backend se expone en `http://localhost:8080`.
* Las migraciones V1-V8 se aplican automáticamente mediante Flyway; Hibernate valida la coherencia del esquema.
* La persistencia se conecta de forma dual: contenedor local o base de datos cloud alojada en InsForge (PostgreSQL en AWS US-East con `sslmode=require`).

---

## Ejecución del Frontend Angular

Desde la carpeta `frontend/`, con Node compatible con Angular 22 (ver `engines` en `package.json`):

```bash
npm ci
npm start
```

* Abrir `http://localhost:4200` en el navegador (el proxy redirige `/api/**` automáticamente al backend en el puerto 8080).
* Para compilar a producción: `npm run build` (genera los artefactos en `dist/frontend/browser/`).
* Para ejecutar las pruebas unitarias: `npm test -- --watch=false` (102 pruebas en 13 suites aprobadas).
* Consulta [la guía del frontend](frontend/README.md) para más detalles técnicos.
