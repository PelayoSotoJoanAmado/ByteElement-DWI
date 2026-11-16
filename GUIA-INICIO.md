# Como clonar y probar StockGuard

Esta es la guia de entrada para quien recibe el proyecto. Backend y frontend se ejecutan en
dos terminales. La base es PostgreSQL; no se necesita InsForge ni activar el perfil demo.

Una vez abierto, seguir [GUIA-USO-SISTEMA.md](GUIA-USO-SISTEMA.md) para conocer los módulos,
los permisos de cada rol y los pasos de inventario, compras, conteos e indicadores.

## 1. Requisitos

| Herramienta | Version / uso |
|---|---|
| Git | Clonar la rama front |
| JDK Java | 21; no basta instalar solo un JRE |
| Node.js | Compatible con frontend/package.json: ^22.22.3, ^24.15.0 o >=26.0.0 |
| npm | Incluido con Node; instalar dependencias con npm ci |
| PostgreSQL | Servidor encendido, base y credenciales propias |

Se comprobaron Java 21.0.10, PostgreSQL 18.1 y Node 24.22.1. Despues de actualizar el equipo,
tambien pasaron las 97 pruebas Angular y el build con Node 26.13.1 / npm 11.20.0.
El compose opcional del repositorio usa PostgreSQL 16; no se acredita aqui una ejecucion con ese motor.
No hace falta instalar Maven ni Angular CLI globales: se usan mvnw.cmd y las dependencias del proyecto.

En una terminal nueva:

```powershell
git --version
java -version
node --version
npm --version
```

Si acabas de instalar Node, abrir otra terminal o reiniciar la terminal del editor para actualizar PATH.

## 2. Clonar la rama correcta

```powershell
git clone --branch front https://github.com/PelayoSotoJoanAmado/ByteElement-DWI.git
cd ByteElement-DWI
```

Si ya tienes una copia, actualizar front cuando tu trabajo local este guardado:

```powershell
git switch front
git pull --ff-only origin front
```

Git trae codigo, migraciones y ejemplos de configuracion. No trae PostgreSQL, los datos privados
del desarrollador, node_modules ni backend/application-local.properties.

## 3. Elegir la base

### A. Primera instalacion del companero

Crear un usuario PostgreSQL con permiso de conexion y una base propia. Ejemplo de nombres:
usuario stockguard, base stockguard y puerto 5432. En pgAdmin se puede crear el usuario con una
contrasena privada y luego la base asignandole ese propietario. Ese usuario debe poder crear
las tablas e indices de Flyway en el esquema public.

No crear manualmente tablas de productos/usuarios: Flyway las crea al arrancar el backend.
No usar como password de PostgreSQL la contrasena de un usuario de la aplicacion: son cuentas distintas.

El compose.yaml de la raiz es una alternativa si se usa Docker. Tambien ocupa localhost:5432;
no arrancarlo a la vez que otro PostgreSQL que ya utiliza ese puerto.

### B. Ya tienes una base con datos, como byteelement

Conservarla y hacer un respaldo antes de actualizar. No crear otra por obligacion, borrar tablas
ni volver a cargar usuarios. Mantener backend/application-local.properties con su conexion actual.
Al arrancar esta version, Flyway aplica solo las migraciones pendientes; V8 conserva las cuentas.

En el equipo revisado, byteelement ya tenia seis cuentas y estaba en V7. Una copia real se migro
correctamente a V8, conservando nombres, hashes, roles, estados y registros. La revision no
migro la base original; el siguiente arranque habitual la actualizara si todavia sigue en V7.

Si quieres que otra persona tenga tambien tus productos/movimientos, necesita un respaldo privado
y restaurarlo en su propia base. Clonar Git no copia esos datos. No agregar dumps al repositorio.

## 4. Configuracion privada del backend

Desde la raiz del proyecto:

```powershell
cd backend
if (-not (Test-Path application-local.properties)) {
    Copy-Item application-local.properties.example application-local.properties
}
```

Editar backend/application-local.properties. No sobrescribirlo si ya funciona; no editar para
este fin el archivo de src/main/resources, que solo contiene la directiva de importacion.

Completar:

| Propiedad | Valor que elige cada responsable |
|---|---|
| spring.datasource.url | jdbc:postgresql://localhost:5432/stockguard, o su host/puerto/base reales |
| spring.datasource.username | Usuario PostgreSQL con acceso a esa base |
| spring.datasource.password | Password de ese usuario PostgreSQL |
| stockguard.jwt.secret | Clave aleatoria privada de al menos 32 bytes |
| stockguard.bootstrap.password | Password temporal inicial del Administrador |
| stockguard.bootstrap.almacen.password | Password temporal inicial de Almacen |
| stockguard.bootstrap.compras.password | Password temporal inicial de Compras |
| stockguard.bootstrap.supervisor.password | Password temporal inicial de Supervisor |
| stockguard.bootstrap.consulta.password | Password temporal inicial de Consulta Comercial |

Las cinco claves iniciales se necesitan en una base vacia y deben ser distintas, de minimo
12 caracteres y maximo 72 bytes UTF-8. Reemplazar los marcadores REEMPLAZAR; no son credenciales
para iniciar sesion. En una base que ya tiene usuarios no reemplazan sus passwords existentes.
stockguard.demo.password solo se utiliza si se activa demo; no es necesario para el arranque normal.

Para generar JWT_SECRET en Windows PowerShell, ejecutar en privado y copiar el resultado al archivo:

```powershell
$bytes = New-Object byte[] 48
$generador = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$generador.GetBytes($bytes)
$generador.Dispose()
[Convert]::ToBase64String($bytes)
```

El archivo privado esta ignorado por Git. No copiar esos valores a Angular ni enviarlos por WhatsApp.

## 5. Terminal 1: arrancar el backend

Estando en backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Esperar a que la consola indique que Spring inicio en el puerto 8080, sin errores de Flyway
o Hibernate. El primer arranque puede descargar dependencias. Dejar la terminal abierta.
El perfil predeterminado es local: no hace falta escribir local,demo ni ejecutar dos modos.
Una terminal nueva evita heredar variables de pruebas, prod o demo de sesiones anteriores.

El backend usa http://localhost:8080/api/v1. Abrir esa raiz en el navegador no es una prueba
de salud: los endpoints estan protegidos. Comprobar Estado del sistema dentro de la aplicacion
con Administrador o Supervisor, despues de completar cualquier cambio de password pendiente.

## 6. Terminal 2: arrancar Angular

Abrir otra terminal en la raiz ByteElement-DWI:

```powershell
cd frontend
npm ci
npm start
```

npm ci instala lo fijado por package-lock.json; hacerlo la primera vez, cuando cambie el lockfile
o al reinstalar dependencias tras cambiar de version principal de Node. En los arranques habituales
basta npm start. Dejar tambien esta terminal abierta.

Abrir **http://localhost:4200**. El proxy de Angular reenvia /api/ al backend local del puerto 8080.
Usar localhost, porque es el origen CORS permitido; 127.0.0.1:4200 es otro origen.

## 7. Con que cuenta entrar

### Base nueva

El backend crea una cuenta por rol durante la primera instalacion, con los nombres predeterminados:

| Usuario | Rol | Password para el primer acceso |
|---|---|---|
| administrador | ADMINISTRADOR | El configurado en stockguard.bootstrap.password |
| almacen | ALMACEN | El configurado en stockguard.bootstrap.almacen.password |
| compras | COMPRAS | El configurado en stockguard.bootstrap.compras.password |
| supervisor | SUPERVISOR | El configurado en stockguard.bootstrap.supervisor.password |
| consulta-comercial | CONSULTA_COMERCIAL | El configurado en stockguard.bootstrap.consulta.password |

Se pueden cambiar esos nombres en la plantilla. No existe una clave publica universal.
Cada cuenta debe cambiar su clave temporal antes de entrar a su panel. Desde entonces inicia
con su nueva clave; editar la configuracion inicial no la cambia ni la restablece.

La instalacion normal crea tambien A01/A02/A03, pero no productos, movimientos ni compras de ejemplo.
El administrador debe completar el catalogo para empezar a operar.

### Base existente del equipo

Entrar con el username y password vigentes. Las cuentas ya creadas no se sustituyen por las
cinco anteriores. Por ejemplo, demo-almacen sigue llamandose demo-almacen hasta que un
administrador cambie su nombre. No es necesario activar demo para entrar con ella.
Las cuentas previas a V8 no reciben un cambio obligatorio retroactivo; Mi cuenta permite el
cambio voluntario. Los tokens anteriores a V8 exigen iniciar sesion de nuevo.

## 8. Recorrido minimo para probar

1. Administrador: entrar, revisar Estado del sistema y crear un producto.
2. Supervisor: configurar minimo/maximo de ese producto y almacen.
3. Almacen: registrar entrada/salida y comprobar existencias.
4. Compras: consultar reposicion y enviar una solicitud con monto y motivo.
5. Supervisor: aprobar; si supera S/5000, indicar justificacion.
6. Compras: emitir orden de la solicitud aprobada.
7. Almacen: recibir parcial/completamente y comprobar que el stock aumenta una sola vez.
8. Consulta Comercial: consultar existencias, sin botones de administracion o compra.
9. Almacen registra conteo; Supervisor confirma el ajuste, genera corte y consulta indicadores.

Cada paso usa su cuenta. Administrador gestiona el sistema y no ejecuta todas las operaciones
como superusuario. Detalle del recorrido y rechazos ya comprobados:
[revision-integrada](backend/docs/APF3/revision-integrada/README.md).

## 9. Pruebas automatizadas opcionales

Desde backend, las pruebas predeterminadas usan H2 de pruebas:

```powershell
.\docs\APF3\Verificar-APF3.ps1
```

No configurar TEST_DB_URL, DEMO_TEST_DB_URL o INSTALACION_TEST_DB_URL con una base de uso real.
Para probar PostgreSQL se necesitan tres bases exclusivas y seguir la guia de revision.
Desde frontend:

```powershell
npm test -- --watch=false
npm run build
```

Los recorridos de navegador crean registros y solo deben ejecutarse en una base aislada:
[frontend/e2e/README.md](frontend/e2e/README.md). No son necesarios para simplemente abrir la aplicacion.

## Problemas de arranque

| Mensaje / sintoma | Comprobar |
|---|---|
| Angular exige otra version de Node | node --version en esa terminal; reiniciar el editor si conserva PATH antiguo |
| No existe @angular/build o ng | Ejecutar npm ci desde frontend |
| npm.ps1 bloqueado en PowerShell | Usar npm.cmd ci / npm.cmd start sin cambiar globalmente la politica de ejecucion |
| Conexion PostgreSQL rechazada | Servidor encendido, host/puerto y base de la configuracion privada |
| Password authentication failed | Password del usuario PostgreSQL; no la clave de un usuario StockGuard |
| Instalacion exige passwords iniciales | Base vacia: completar las cinco claves distintas antes de arrancar |
| Ya cambiaste la clave y no acepta la del archivo | Usar la clave vigente; bootstrap no restablece cuentas existentes |
| Login devuelve 403 desde 127.0.0.1 | Abrir localhost:4200 o configurar expresamente CORS_ORIGINS |
| Puerto 8080/4200 ocupado | Detener el proceso propio anterior; no arrancar otra instancia encima |
| Flyway falla en una base existente | Revisar destino e historial; no borrar tablas ni usar repair/baseline para ocultar el error |

Si aparece UNABLE_TO_VERIFY_LEAF_SIGNATURE al instalar npm en Windows, usar los certificados
de confianza del sistema en esa terminal y repetir npm ci:

```powershell
$env:NODE_USE_SYSTEM_CA = '1'
npm ci
```

No desactivar la validacion TLS. Para despliegue o InsForge, seguir las guias de APF3; no son
pasos requeridos para probar el proyecto localmente.
