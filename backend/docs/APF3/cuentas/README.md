# Instalacion y gestion de cuentas

Actualizacion del 16 de noviembre de 2026. Este documento reemplaza las instrucciones anteriores
de APF3 que describian solo un administrador inicial y usuarios sin edicion.
Los permisos operativos de los cinco roles se conservan. No se agrega SUPERADMIN.

## Comportamiento de una instalacion nueva

1. Flyway aplica V1-V8. Hibernate valida.
2. Si no hay usuarios y la instalacion no fue completada, el backend valida cinco nombres y
   cinco passwords privados distintos antes de insertar nada.
3. Crea una cuenta activa de cada rol, con cambio de password obligatorio y evento de auditoria.
4. No crea productos, movimientos ni compras. No hace falta activar local,demo.
5. Dos instancias que arranquen simultaneamente no duplican ni dividen la instalacion.
6. Tras instalar, reiniciar o cambiar las variables iniciales no cambia cuentas/passwords ni
   reactiva cuentas desactivadas. Una base existente no recibe nuevas cuentas automaticamente.

Cada persona entra con las credenciales iniciales elegidas por el responsable de instalacion.
Hasta cambiarlas solo puede consultar Mi cuenta y cambiar su password. El servidor impide las
demas operaciones, aunque alguien altere el menu o las banderas de Angular.

## Configuracion local

Editar backend/application-local.properties, no el archivo del mismo nombre en src/main/resources.
La plantilla backend/application-local.properties.example contiene las claves necesarias.
No sobrescribir una configuracion existente al copiar la plantilla.

| Rol | Nombre inicial predeterminado | Propiedad privada de password |
|---|---|---|
| Administrador | administrador | stockguard.bootstrap.password |
| Almacen | almacen | stockguard.bootstrap.almacen.password |
| Compras | compras | stockguard.bootstrap.compras.password |
| Supervisor | supervisor | stockguard.bootstrap.supervisor.password |
| Consulta comercial | consulta-comercial | stockguard.bootstrap.consulta.password |

Administrador usa stockguard.bootstrap.username para el nombre. Los otros nombres se configuran
con stockguard.bootstrap.almacen.username, compras.username, supervisor.username y consulta.username.
Nombres unicos de 3..80 caracteres: letras ASCII, numeros, punto, guion y guion bajo.
No son correos. Los nombres distinguen mayusculas/minusculas.
Passwords distintos, no vacios, de al menos 12 caracteres y como maximo 72 bytes UTF-8.
No usar los marcadores REEMPLAZAR de la plantilla; se rechazan durante la instalacion.
Las contrasenas se guardan en la base como hashes BCrypt, no como texto recuperable.

Desde backend, con Java 21 y PostgreSQL configurados:

```powershell
.\mvnw.cmd spring-boot:run
```

El perfil predeterminado local importa el archivo privado. Si la terminal conserva el perfil demo
de una sesion anterior, seleccionar local o abrir una terminal nueva antes de arrancar.
stockguard.bootstrap.enabled=true es el valor predeterminado. Deshabilitarlo no crea cuentas:
solo usar false cuando la instalacion esta provisionada por un mecanismo controlado, o en pruebas.

## Despliegue

Configurar secretos en el alojamiento del backend:

| Variable | Contenido |
|---|---|
| SPRING_PROFILES_ACTIVE | prod |
| DEBUG | false |
| DB_URL, DB_USER, DB_PASSWORD | Conexion PostgreSQL |
| JWT_SECRET | Clave privada estable de firma de al menos 32 bytes |
| BOOTSTRAP_USER, BOOTSTRAP_PASSWORD | Administrador inicial |
| BOOTSTRAP_ALMACEN_USER, BOOTSTRAP_ALMACEN_PASSWORD | Cuenta inicial Almacen |
| BOOTSTRAP_COMPRAS_USER, BOOTSTRAP_COMPRAS_PASSWORD | Cuenta inicial Compras |
| BOOTSTRAP_SUPERVISOR_USER, BOOTSTRAP_SUPERVISOR_PASSWORD | Cuenta inicial Supervisor |
| BOOTSTRAP_CONSULTA_USER, BOOTSTRAP_CONSULTA_PASSWORD | Cuenta inicial Consulta |
| CORS_ORIGINS | Origen del frontend |

Los nombres USER de las cuatro cuentas operativas son optativos si se aceptan los predeterminados;
sus passwords no tienen valores publicos de reserva. BOOTSTRAP_USER tambien es optativo si se acepta administrador.
Las claves iniciales solo se requieren al instalar una base vacia; pueden retirarse una vez cambiadas
y completada la instalacion. Conservar los secretos de conexion y JWT que usa el servidor.
No guardar estas credenciales en Git, Angular, capturas ni colecciones exportadas.
InsForge puede incorporarse posteriormente sin cambiar este flujo.

## Bases existentes y las cuentas actuales del equipo

V8 conserva usernames, hashes, roles y estados activos existentes. No renombra demo-*, no agrega
otro juego de cuentas, no fuerza un cambio masivo de passwords y no reemplaza credenciales.
El nuevo campo requiere_cambio_password comienza false para cuentas previas a V8.
El Administrador puede renombrar cuentas ajenas o crear nuevas si quieren abandonar los nombres demo.
Usar Mi cuenta para cambiar el password propio. Para otros usuarios, restablecer una clave temporal
desde Administracion; esa cuenta debera cambiarla en el siguiente acceso.
No usar una clave demo compartida como credencial permanente del sistema final.

Los JWT anteriores a esta version dejan de aceptarse: cerrar sesion e iniciar de nuevo.
Los nuevos tokens contienen id de usuario y version de sesion ademas del rol.
Un cambio/restablecimiento de password, edicion de cuenta o desactivacion revoca los tokens anteriores.
La reactivacion no vuelve a validar los tokens viejos. La revocacion se verifica en cada peticion
posterior; no cancela operaciones HTTP que ya estaban ejecutandose.

local,demo sigue siendo optativo y puede crear cuentas demo adicionales y datos de ejemplo.
Las cuentas demo nuevas tambien requieren cambio inicial; las existentes no se sobreescriben.
Desactivar ese perfil no borra cuentas ni datos. El funcionamiento normal no depende de demo.

## Gestion de cuentas

- Todos los roles: consultar Mi cuenta y cambiar password propio indicando el actual.
- Administrador con clave definitiva: crear/listar/editar cuentas ajenas, activar/desactivar,
  asignar rol, restablecer password temporal y consultar auditoria.
- Cada cuenta mantiene un solo rol. No se agrega multirrol ni se amplian permisos de compras/inventario.
- Administrador no puede administrarse a si mismo desde Usuarios: no puede cambiar su propio rol,
  desactivarse, renombrarse ni usar el restablecimiento administrativo sobre si mismo.
  Su cambio de password se hace en Mi cuenta.
- Las escrituras de administracion requieren version del registro y motivo; una version obsoleta
  produce 409. Una edicion concurrente no debe sobrescribir silenciosamente a otra.
- Se conserva al menos un administrador activo. El control tambien cubre administradores
  que intenten desactivarse mutuamente en peticiones simultaneas.
- No hay borrado fisico de usuarios ni recuperacion publica por email.
- Si se pierde el acceso al unico administrador, se requiere recuperacion tecnica controlada.
  Cambiar el archivo de instalacion, activar demo o borrar usuarios no es un mecanismo de recuperacion.

evento_usuario conserva actor/usuario por FK, nombres al momento, tipo, fecha UTC y detalle.
Registra instalacion, creacion, actualizacion, cambio propio y restablecimiento. No agrega hashes,
passwords ni JWT a los eventos. No escribir secretos en el campo motivo.
control_cuentas es una tabla tecnica de instalacion y bloqueo, no un sexto rol.

## Guia del frontend y verificacion

Entregar al responsable Angular [CONTRATO-FRONTEND.md](CONTRATO-FRONTEND.md).
Consultar [CAMBIOS-INFORME.md](CAMBIOS-INFORME.md) para el informe.
Resultados y reproduccion en [VALIDACION.md](VALIDACION.md).
Las pantallas Angular de cuentas ya se integraron. Su validacion local con backend/PostgreSQL
esta en [revision-integrada/README.md](../revision-integrada/README.md); conservar los limites de esa evidencia.
