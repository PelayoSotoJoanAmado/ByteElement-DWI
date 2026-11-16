# Incorporar InsForge a StockGuard

Guia preparada el 13 de noviembre de 2026 para realizar la conexion cuando el equipo la necesite.
Esta guia no configura una cuenta ni cambia la base que usa actualmente el proyecto.
La conexion debe probarse con las credenciales reales del proyecto elegido.

## Que cambia

InsForge Cloud proporciona PostgreSQL con conexion directa. Spring Boot usa esa conexion por JDBC
y conserva JPA/Hibernate, repositorios, transacciones, JWT/RBAC y migraciones Flyway.
La disponibilidad de la conexion directa se documenta en la [FAQ oficial](https://docs.insforge.dev/faq).

Arquitectura:

    Angular -> API Spring Boot -> JPA/Hibernate -> PostgreSQL en InsForge

Angular sigue usando la URL de Spring Boot. Elegir InsForge como base no publica automaticamente
el backend Java ni el frontend. Para desarrollo, Spring Boot y Angular pueden seguir corriendo
en tu equipo mientras la base esta alojada.

## 1 Elegir si empezar vacio o conservar datos

| Situacion | Procedimiento |
|---|---|
| Solo quieres conectar una base nueva | Configurar la conexion y permitir que Flyway cree StockGuard |
| Quieres conservar productos, usuarios y operaciones actuales | Exportar las tablas StockGuard y restaurarlas junto con flyway_schema_history antes de arrancar |

Elegir una alternativa. No crear primero las tablas con Flyway y luego restaurar encima el mismo esquema.
La carga de 10000 productos es opcional; no es necesaria para iniciar Angular.

## 2 Obtener la conexion PostgreSQL

Crear o elegir un proyecto InsForge Cloud y abrir Settings -> Connect -> Connection String.
Obtener host, puerto, nombre de base, usuario y password. No confundir:

- La URL HTTPS del proyecto/API de InsForge con el host PostgreSQL.
- La API key de InsForge con el password de PostgreSQL.
- Un JWT de usuario de StockGuard con una credencial de base.

Si ya usas la CLI y el proyecto esta vinculado:

```powershell
npx -y @insforge/cli current
npx -y @insforge/cli db connection-string
```

La salida contiene un secreto; usarla en privado. Si no hay proyecto vinculado, elegir/vincular
el proyecto correcto antes de consultar la cadena. Para esta conexion JDBC no hace falta agregar
el SDK de InsForge a Angular ni reemplazar los servicios Java.

## 3 Configurar Spring Boot

Convertir la conexion a formato JDBC, separando usuario/password:

```text
jdbc:postgresql://HOST:PUERTO/BASE?sslmode=require
```

HOST, PUERTO y BASE deben ser los valores reales obtenidos. No utilizar localhost para la base remota.
El driver PostgreSQL ya esta incluido en pom.xml. Mantener ddl-auto=validate y Flyway habilitado.

Desde backend, ejemplo para Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:DEBUG = 'false'
$env:DB_URL = Read-Host 'URL JDBC PostgreSQL sin usuario ni password'
$env:DB_USER = Read-Host 'Usuario PostgreSQL'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Password PostgreSQL' -AsSecureString)).Password

$bytes = New-Object byte[] 48
$generador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$generador.GetBytes($bytes)
$generador.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)

$env:BOOTSTRAP_USER = 'administrador'
$env:BOOTSTRAP_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Password del primer administrador' -AsSecureString)).Password
$env:BOOTSTRAP_ALMACEN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Password inicial Almacen' -AsSecureString)).Password
$env:BOOTSTRAP_COMPRAS_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Password inicial Compras' -AsSecureString)).Password
$env:BOOTSTRAP_SUPERVISOR_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Password inicial Supervisor' -AsSecureString)).Password
$env:BOOTSTRAP_CONSULTA_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Password inicial Consulta' -AsSecureString)).Password
$env:CORS_ORIGINS = 'http://localhost:4200'
```

Los cinco passwords deben ser privados, distintos, de al menos 12 caracteres y como maximo 72 bytes UTF-8.
En una base nueva se crean cinco cuentas, una por rol, con cambio obligatorio. No se requiere demo.
Ver nombres y configuracion en [cuentas/README.md](cuentas/README.md).
Si restauraste cuentas, se conservan sus passwords y roles; esas variables no cambian cuentas existentes.
Al restaurar datos conviene conservar el JWT_SECRET del backend original o decidir renovar las sesiones.
Con varias instancias de la misma API, compartir su clave de firma mediante secretos del alojamiento.

En un despliegue, cargar estas variables en el gestor de secretos/configuracion del alojamiento Java.
No guardar las credenciales en archivos versionados, TypeScript, capturas ni colecciones exportadas.
El perfil prod no importa el archivo local de secretos del desarrollador.

Para usar posteriormente la aplicacion local otra vez, abrir otra terminal o quitar las variables
anteriores y seleccionar el perfil local. No dejar DB_URL apuntando a la nube al hacer pruebas locales.

## 4A Crear StockGuard en una base nueva

Con el proyecto destino correcto y sin tablas StockGuard existentes:

```powershell
.\mvnw.cmd spring-boot:run
```

Comprobar en consola la URL de destino sin publicar passwords. Flyway aplica V1-V8 y Hibernate
valida las entidades. Se crean los tres almacenes y las cinco cuentas iniciales, sin activar demo.
Completar el cambio de password de cada una antes de operar. Las cuentas adicionales se crean
con POST /api/v1/usuarios usando un administrador con password definitivo.

Un proyecto del proveedor puede contener objetos propios. Si Flyway informa que public no esta
vacio pero no tiene historial, detenerse y revisar que objetos existen. No activar baselineOnMigrate
con su version predeterminada: podria omitir V1. La adopcion de un esquema no vacio requiere un
procedimiento revisado; no borrar tablas del proveedor ni crear flyway_schema_history a mano.

Flyway sigue siendo la autoridad del esquema StockGuard. No aplicar V1-V8 tambien desde otro
gestor de migraciones ni editar las migraciones ya aplicadas.

## 4B Conservar los datos actuales

Hacer primero una copia y ensayar sobre un proyecto/base de destino separado.
Detener temporalmente escrituras de StockGuard durante el traslado final.
El destino debe carecer de las tablas StockGuard y de su historial Flyway.

Exportar solo objetos de la aplicacion: no volcar/restaurar esquemas administrados por InsForge,
como auth, storage o realtime. Incluir flyway_schema_history para conservar las versiones/checksums.

Ejemplo conceptual con clientes PostgreSQL disponibles en PATH; sustituir nombres de conexion
y definir PGPASSWORD para el origen o usar pgpass.conf antes de ejecutar:

```powershell
$tablas = @(
    'producto','almacen','politica_stock','usuario','existencia','movimiento',
    'conteo','corte_stock','solicitud_compra','orden_compra','recepcion_compra',
    'control_cuentas','evento_usuario',
    'flyway_schema_history'
)
$argumentos = @('-h','HOST_ORIGEN','-p','PUERTO_ORIGEN','-U','USUARIO_ORIGEN','-d','BASE_ORIGEN','-Fc','-f','stockguard.dump')
foreach ($tabla in $tablas) { $argumentos += @('-t',"public.$tabla") }
pg_dump @argumentos
if ($LASTEXITCODE -ne 0) { throw 'La exportacion fallo' }
```

Para versiones anteriores a V6, recepcion_compra no existe; antes de V8 tampoco existen
control_cuentas y evento_usuario: adaptar la lista al esquema real. Al restaurar una base V7,
Flyway aplicara V8 conservando credenciales y estados existentes.
Usar clientes compatibles con la version origen/destino. Un pg_dump mas antiguo que el servidor
origen no sirve para exportarlo. Una restauracion hacia un PostgreSQL mas antiguo requiere revisar compatibilidad.

Configurar las credenciales del destino y exigir SSL antes de restaurar:

```powershell
$env:PGSSLMODE = 'require'
$env:PGPASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Password PostgreSQL destino' -AsSecureString)).Password
pg_restore -h HOST_DESTINO -p PUERTO_DESTINO -U USUARIO_DESTINO -d BASE_DESTINO --no-owner --no-acl --single-transaction stockguard.dump
if ($LASTEXITCODE -ne 0) { throw 'La restauracion fallo; no arrancar la aplicacion sobre una copia incompleta' }
Remove-Item Env:PGPASSWORD
```

El backup contiene datos de usuarios y del negocio; guardarlo fuera del repositorio o en una
carpeta ignorada. Comprobar antes los permisos que usara el usuario JDBC despues de restaurar.
Luego arrancar el backend con la conexion de destino: Flyway valida el historial conservado y
aplica solo las migraciones pendientes. Si no coincide un checksum, investigar la diferencia;
no usar repair ni baseline para ocultarla.

## 5 Comprobar antes de dar la conexion por terminada

1. Confirmar host/base en el log de arranque, migraciones correctas y ausencia de errores de validacion.
2. Login con un usuario existente o con una cuenta inicial. Si requiereCambioPassword=true,
   completar POST /api/v1/auth/cambiar-password y usar el token NUEVO antes de consultar modulos.
3. Consultar GET /api/v1/salud con Supervisor o Administrador.
4. Consultar productos y existencias; comprobar conteos y saldos contra el origen si hubo traslado.
5. Probar una operacion nueva en datos de control: verifica tambien que los IDs/secuencias siguen avanzando.
6. Recorrer solicitud -> aprobacion -> orden -> recepcion; verificar saldo y movimiento.
7. Comprobar conteo/ajuste, dashboard con periodo/corte conocidos y denegacion por rol.
8. Medir latencias desde donde correra el backend, no solo desde el editor SQL del proveedor.

No apuntar TEST_DB_URL, DEMO_TEST_DB_URL ni INSTALACION_TEST_DB_URL a la base compartida.
La suite usa tres bases dedicadas; la de instalacion limpia sus propias cuentas entre casos.
Tampoco ejecutar Cargar-Volumen.ps1 en una base de operacion.

## 6 Acceso y capacidad

La seguridad de StockGuard sigue en Spring. Revisar que las tablas de negocio no tengan un
acceso alternativo publico mediante los endpoints REST/SDK del proveedor. No conceder permisos
a anon/authenticated para que Angular salte la API Java. El usuario JDBC debe disponer de los
permisos necesarios para Flyway y la aplicacion; despues se puede separar migrador y usuario de runtime.
La [documentacion de base de InsForge](https://docs.insforge.dev/core-concepts/database/overview)
describe sus endpoints y RLS; no asumir que un JWT de StockGuard configura automaticamente esas politicas.

Consultar las conexiones permitidas por el plan. Como punto inicial conservador, puede configurarse
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5 y medir; cinco conexiones no son una cuota oficial del proveedor.
El total se multiplica por las instancias de backend y compite con otros clientes.
Los 10000 productos no fijan por si solos el consumo: movimientos y cortes hacen crecer el historial.
Revisar almacenamiento, pausas, copias y limites vigentes en los [planes oficiales](https://insforge.dev/pricing)
antes de elegir el entorno de la sustentacion.

## Problemas habituales

| Sintoma | Revisar |
|---|---|
| Sigue conectando a localhost | Perfil activo, DB_URL y variables SPRING_DATASOURCE_* que puedan prevalecer |
| Password incorrecto | Credencial PostgreSQL real; no usar API key ni JWT |
| No conecta por red | Host/puerto de la cadena, proyecto disponible, DNS y conectividad desde el backend |
| Error SSL | Parametros TLS indicados por el proveedor y certificados; no deshabilitar la validacion para ocultar el error |
| Flyway encuentra tablas sin historial | Si se restauro flyway_schema_history y si el destino era apropiado |
| Login falla tras restaurar | Cuentas conservadas; bootstrap no reemplaza usuarios existentes |
| Dashboard sin datos | Periodo elegido, corte dentro del periodo y politicas de los pares criticos |
| Angular recibe CORS/404 | URL publica de Spring y CORS_ORIGINS; la URL InsForge no es la API Java |

Si hay un problema, detener el cambio de conexion y volver a la configuracion local anterior.
Conservar la base original hasta verificar el destino; no requiere reemplazar el backend.
