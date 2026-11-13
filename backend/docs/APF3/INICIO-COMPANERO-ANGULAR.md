# Inicio para el responsable de Angular

El backend implementa los contratos de catalogo, inventario, reposicion, compras, conteos e indicadores.
Puedes comenzar Angular sin configurar InsForge. El cierre integrado requiere ejecutar F01-F05
desde el navegador y corregir cualquier incidencia que aparezca al conectar ambas aplicaciones.

## Recibir el proyecto correcto

La rama de trabajo es prueba. Recibir el commit de APF3 y actualizar esa rama antes de empezar;
no trabajar solo con main, que conserva un corte anterior. El commit de entrega se identifica
por su mensaje feat(apf3): completar backend y documentar integracion Angular.
No compartir application-local.properties, contrasenas, JWT_SECRET ni los archivos de target.
Cada integrante configura su entorno local desde la plantilla.

## Leer en este orden

1. HANDOVER_ESTADO_PROYECTO.md en la raiz: leer primero la actualizacion APF3; el corte APF2 inferior es historico.
2. [API-ANGULAR.md](API-ANGULAR.md): compras, reposicion, dashboard, permisos, errores, JWT e idempotencia.
3. [APF2-MINIMO.md](../APF2/APF2-MINIMO.md): contratos conservados de productos, almacenes, movimientos,
   transferencias, politicas, conteos y cortes. Sus pendientes de compras ya fueron resueltos en APF3.
4. [REGLAS-Y-MODELO.md](REGLAS-Y-MODELO.md): reglas que deben reflejar formularios y mensajes.
5. [OPERACION.md](OPERACION.md) y [VALIDACION.md](VALIDACION.md): arranque y evidencia comprobada.

Las definiciones exactas de entrada/salida estan en backend/src/main/java/com/byteelement/stockguard/dto.
Para compras y reposicion, revisar ComprasDtos.java; para indicadores, IndicadorResponse.java.
Usar DTOs para las interfaces TypeScript; no exponer entidades JPA como contratos del cliente.
La coleccion StockGuard-APF3.postman_collection.json sirve para comprobar la API antes de maquetar.

## Arranque minimo en tu equipo

Requisitos: Java 21, PostgreSQL (o Docker para el compose existente) y el entorno de Angular.
Crear/iniciar una base local. Desde backend, si aun no existe tu configuracion:

```powershell
Copy-Item application-local.properties.example application-local.properties
```

Editar esa copia: URL/usuario/password de PostgreSQL, JWT_SECRET de al menos 32 bytes,
primer administrador y contrasena de demostracion. No conservar los valores REEMPLAZAR.
El primer administrador solo se crea cuando la base no tiene usuarios.
Usar una base de desarrollo vacia para comenzar con el escenario pequeno:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local,demo'
$env:DEBUG = 'false'
.\mvnw.cmd spring-boot:run
```

Flyway aplica V1-V7 y Hibernate valida. No crear tablas manualmente ni cambiar ddl-auto.
El perfil demo crea dos productos, politicas y operaciones de control; no es la carga de 10000 productos.
Los usuarios demo comparten la contrasena que configuraste en stockguard.demo.password:

| Usuario | Rol |
|---|---|
| demo-administrador | ADMINISTRADOR |
| demo-almacen | ALMACEN |
| demo-compras | COMPRAS |
| demo-supervisor | SUPERVISOR |
| demo-consulta_comercial | CONSULTA_COMERCIAL |

Sin demo tambien puedes usar el administrador inicial y crear cuentas mediante POST /api/v1/usuarios.
Las bases de volumen y los servidores temporales usados para validar no se entregan funcionando;
los scripts y datos para recrearlos estan en esta carpeta. No necesitas la carga masiva para comenzar la interfaz.

## Conexion del frontend

- Angular local: http://localhost:4200.
- API local: http://localhost:8080/api/v1.
- Configurar apiBaseUrl en el cliente; no escribir URLs distintas en cada componente.
- Cada desarrollador que usa localhost necesita su propio backend levantado.
- Una base alojada en InsForge no publica la API Java. Para consumir un unico backend remoto,
  tambien hay que alojar Spring Boot y configurar su URL/CORS.
- El cliente nunca recibe credenciales de PostgreSQL, la clave de firma JWT ni claves administrativas de InsForge.

## Orden de implementacion sugerido

1. Sesion: login, usuario/rol, interceptor Bearer, expiracion/401 y guards.
2. Catalogo y consulta de saldo; mantenimiento por administrador y politicas por supervisor.
3. Entradas, salidas y transferencias; kardex con filtros/paginacion.
4. Reposicion: regla, consumo, pendientes, alertas y sugerencia. COMPRAS envia la solicitud.
5. Compras: advertencias, confirmacion de duplicidad, decision del supervisor, emision por Compras,
   recepcion parcial por Almacen y cancelacion del pendiente por Supervisor.
6. Conteos: registrar, comparar saldo al corte y confirmar ajuste por supervisor.
7. Dashboard I05-I10, reportes y evidencias tecnicas para administrador.

Mantener core para autenticacion/HTTP/configuracion, shared para elementos comunes y features
para las pantallas. Utilizar rutas y formularios con validacion y estilos SASS.
No duplicar los calculos de stock ni indicadores en Angular: mostrar los resultados de la API.

## Detalles que suelen causar errores

- Paginacion base 0; tamanio maximo 100; mostrar content y totalElements.
- Fechas ISO-8601 con zona; hasta exclusivo.
- No convertir valor=null/sinDatos=true a 0%.
- I08 es dias y su numerador representa segundos.
- I05/I10 necesitan un corte dentro del periodo; I10 exige politicas en los pares criticos seleccionados.
- Generar claves de idempotencia para solicitudes/recepciones y conservarlas al reintentar.
- El reintento de creacion devuelve la respuesta original; GET devuelve el estado actual.
- Manejar 400/401/403/404/409 con mensajes y estados de carga; permisos tambien se verifican en el servidor.
- Las plantillas Hola mundo de resources/templates no son el frontend Angular y no deben usarse como base de vistas.

## Entrega integrada

Comprobar desde Angular: F01 movimiento, F02 transferencia, F03 reposicion/compra/recepcion,
F04 conteo/ajuste y F05 dashboard/permisos. Demostrar casos correctos y rechazos, persistencia,
filtros y numeros verificables. Registrar la version integrada y completar el informe con
[CAMBIOS-INFORME.md](CAMBIOS-INFORME.md).

Texto breve para una IA que ayude con el frontend:

> Trabaja en StockGuard MYPE de ByteElement-DWI. Implementa el frontend Angular con SASS en frontend/.
> Lee primero HANDOVER_ESTADO_PROYECTO.md y backend/docs/APF3/INICIO-COMPANERO-ANGULAR.md.
> Consume la API REST existente siguiendo API-ANGULAR.md y los DTOs Java. El backend usa Spring Boot,
> JPA/Hibernate, PostgreSQL y JWT con cinco roles. Respeta permisos, estados, idempotencia,
> paginacion, fechas y errores. No reemplaces el backend ni accedas a PostgreSQL/InsForge desde Angular.
> Integra y demuestra los cinco flujos F01-F05; conserva documentacion y registra las incidencias reales.
