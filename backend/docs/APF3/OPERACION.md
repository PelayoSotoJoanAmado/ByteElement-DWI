# Operacion y conexion futura

## Arranque local

Requiere Java 21 y PostgreSQL. Mantener secretos en backend/application-local.properties
(ignorado por Git) o en variables de entorno. Desde backend ejecutar .\mvnw.cmd spring-boot:run.
El perfil predeterminado es local y ya no activa demo automaticamente.
Para exponer el escenario pequeno anterior, definir SPRING_PROFILES_ACTIVE=local,demo y DEMO_PASSWORD.
Para compartir, definir DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, BOOTSTRAP_USER,
BOOTSTRAP_PASSWORD, SPRING_PROFILES_ACTIVE=prod y CORS_ORIGINS.
El perfil prod no importa application-local.properties. Solo el perfil local habilita esa importacion.
Definir DEBUG=false en ambientes compartidos; una variable DEBUG del equipo puede activar
trazas de Spring aunque el codigo no las habilite.
No usar ddl-auto=update/create: Flyway migra y Hibernate valida.
La aplicacion es un monolito Spring Boot; el navegador solo consume /api/v1.

## Datos de volumen

Usar una base dedicada y migrada a V7. Crear usuarios activos de Almacen, Compras y Supervisor
desde un administrador por POST /usuarios (o usar demo en una base de exposicion).
Ejecutar Cargar-Volumen.ps1 con nombre de base, usuario, puerto y ubicacion del cliente PostgreSQL.
PGPASSWORD o pgpass.conf suministran la credencial. No guardarla en scripts ni capturas.

La carga es transaccional, sintetica y no borra registros. Rechaza una base con SKU VOL-,
para evitar sumar saldos o compras en una segunda ejecucion. Genera 10000 productos y 154840 filas
relacionadas; incorpora A01/A02/A03. Se carga por SQL para poblar rapidamente el escenario,
sin sustituir las reglas del servicio en la operacion normal.
Valores de control de los ultimos siete dias, con solo este catalogo:
I05=10%, I06=20%, I07=90%, I08=2 dias, I09=8%, I10=10%.
Otros productos, cortes o movimientos de la misma base pueden cambiar los agregados.

Comprobar conteos, saldos negativos, cantidad recibida <= ordenada y ausencia de ordenes
repetidas por solicitud. El lote de corte de esta carga es 00000000-0000-4000-8000-000000010000.

## Rendimiento y monitoreo

Medir-Carga.ps1 recibe BaseUrl, Token, Solicitudes, Ruta y Salida. No escribe el token en el reporte.
Registra promedio, p95, maximo, errores y concurrencia=1. Para concurrencia usar la suite de
integracion (salidas, transferencias, emision y recepciones simultaneas) y una herramienta HTTP
con usuarios independientes. No confundir esas pruebas con un benchmark de produccion.
Primero medir, despues acordar el umbral RNF06 segun el entorno.

RegistroHttp registra metodo/ruta/status/duracion; ComprasService registra IDs de operaciones.
No habilitar DEBUG de Spring Web: puede registrar cuerpos y JWT.
GET /salud comprueba SELECT 1, protegido para Supervisor/Administrador.
Rotacion pagina pares en BD antes de cargar movimientos. Reposicion calcula solo la pagina
solicitada (maximo 100). Los cortes cargan saldos/politicas por lote e I05 reconstruye saldos
iniciales con una consulta, evitando una consulta por producto/almacen.
Los cortes completos siguen bloqueando el catalogo durante la fotografia; ejecutarlos fuera
del pico operativo. Un corte de 10000 productos en tres almacenes agrega 30000 filas.

## Backup y restauracion

Con clientes PostgreSQL compatibles y credenciales en PGPASSWORD/pgpass:

```powershell
pg_dump -h HOST -p PUERTO -U USUARIO -d BASE -Fc -f stockguard-prueba.dump
createdb -h HOST -p PUERTO -U USUARIO stockguard_restaurada
pg_restore -h HOST -p PUERTO -U USUARIO -d stockguard_restaurada --no-owner --single-transaction stockguard-prueba.dump
```

Comparar conteos por tabla, sumas de existencia, cantidades recibidas, claves y referencias;
consultar los seis indicadores contra la restaurada. Registrar motor/version y resultados.
Backup y credenciales no deben subirse al repositorio. Restaurar sobre otra base, sin reemplazar
la compartida durante la demostracion.

## InsForge despues

La guia detallada con ejemplos de configuracion, traslado y comprobacion esta en
[INCORPORAR-INSFORGE.md](INCORPORAR-INSFORGE.md).

No hace falta InsForge para desarrollar. Cuando el equipo decida usarlo:

1. Crear/elegir un proyecto Cloud y obtener conexion PostgreSQL directa desde Settings -> Connect.
   Tambien existe npx -y @insforge/cli db connection-string.
2. Usar formato JDBC: jdbc:postgresql://HOST:5432/BASE?sslmode=require.
   Usuario/password por DB_USER/DB_PASSWORD, nunca en Angular.
3. Configurar DB_URL y secretos en el alojamiento del backend Java.
4. Flyway aplica V1-V7 al iniciar. Si ya existen tablas importadas, revisar su historial;
   no ejecutar baseline ni ignorar checksums para ocultar diferencias.
5. Trasladar datos con pg_dump/pg_restore conservando flyway_schema_history; ensayar antes.
6. Conservar JWT/RBAC de Spring. Evitar acceso alternativo publico a tablas mediante REST/SDK
   del proveedor; las reglas de negocio deben atravesar la API Spring.
7. Ajustar Hikari al limite de conexiones del plan, medir recursos y configurar HTTPS.

Elegir InsForge como base no despliega automaticamente el JAR ni Angular.
Conservar Flyway como autoridad del esquema StockGuard. Consultar limites al desplegar:
https://docs.insforge.dev/faq y https://insforge.dev/pricing.
