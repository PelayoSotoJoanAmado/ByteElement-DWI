# Contrato para implementar cuentas y primer acceso en Angular

Backend actualizado el 16 de noviembre de 2026. Leer antes de modificar el login o Usuarios.
Base /api/v1. JSON. Bearer JWT salvo login. No hay registro publico ni SUPERADMIN.
Las areas de Compras, Supervisor y Consulta siguen siendo trabajo de integracion separado.
La matriz operativa de roles no cambia con esta ampliacion de cuentas.
La asignacion exacta de pestanas, botones y rutas por rol esta en
[ROLES-PERMISOS-Y-PENDIENTES.md](../../../../frontend/docs/ROLES-PERMISOS-Y-PENDIENTES.md).

## 1 Modelo TypeScript

```typescript
type Role = 'ADMINISTRADOR' | 'ALMACEN' | 'COMPRAS' | 'SUPERVISOR' | 'CONSULTA_COMERCIAL';
interface User {
  id: number;
  username: string;
  rol: Role;
  activo: boolean;
  requiereCambioPassword: boolean;
  version: number;
}
interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number; // segundos
  usuario: User;
}
interface UserEvent {
  id: number;
  usuarioId: number;
  actorId: number | null; // null para instalacion/inicializador tecnico
  actorUsername: string; // 'SISTEMA' cuando no hay usuario actor
  usuarioUsername: string;
  tipo: string;
  fecha: string; // ISO UTC
  detalle: string;
}
```

version es revision del registro para administracion, NO version de Angular ni una clave JWT.
No incrementarla en el cliente: conservar la recibida y reemplazarla por la respuesta actualizada.
No existen campos email, nombre completo o foto en el contrato actual.

## 2 Endpoints

| Metodo y ruta | Acceso | Cuerpo / respuesta |
|---|---|---|
| POST /auth/login | Publico, sin Authorization | {username,password} -> LoginResponse |
| GET /auth/me | Cualquier cuenta activa autenticada, incluso cambio pendiente | Sin cuerpo -> User |
| POST /auth/cambiar-password | Cualquier cuenta activa autenticada, incluso cambio pendiente | CambiarPassword -> LoginResponse NUEVO |
| GET /usuarios | Administrador sin cambio pendiente | Sin cuerpo -> User[] |
| POST /usuarios | Administrador sin cambio pendiente | CrearUsuario -> User, status 201 |
| PUT /usuarios/{id} | Administrador sin cambio pendiente, otra cuenta | ActualizarUsuario -> User, status 200 |
| POST /usuarios/{id}/restablecer-password | Administrador sin cambio pendiente, otra cuenta | RestablecerPassword -> User, status 200 |
| GET /usuarios/{id}/eventos | Administrador sin cambio pendiente | pagina=0,tamanio=20 -> Page<UserEvent> |

Las respuestas de login, Mi cuenta y cambio propio llevan Cache-Control: no-store.
La pagina de eventos usa content,totalElements,totalPages,number,size; maximo 100 por pagina.
No existe DELETE /usuarios ni endpoint de recuperacion por email.

## 3 Login y estado de la sesion

Enviar:

```json
{"username":"compras","password":"valor-escrito-por-el-usuario"}
```

Respuesta de ejemplo estructural, sin token ni password reales:

```json
{
  "accessToken":"TOKEN_DEVUELTO_POR_EL_SERVIDOR",
  "tokenType":"Bearer",
  "expiresIn":1800,
  "usuario":{
    "id":3,"username":"compras","rol":"COMPRAS","activo":true,
    "requiereCambioPassword":true,"version":0
  }
}
```

Login con cambio pendiente SI devuelve 200 y un token: ese token tiene acceso limitado
para consultar Mi cuenta y cambiar password. No significa que pueda operar en los modulos.
Si requiereCambioPassword=true, navegar ANTES de cargar cualquier panel a la ruta propuesta
/cuenta/cambiar-password. No mandar al usuario a su panel para despues esperar un 403.

Al restaurar una sesion del almacenamiento, consultar GET /auth/me para refrescar estado.
No confiar en el rol/flag modificado manualmente en sessionStorage. El servidor lo controla
con datos vigentes en la base y revoca tokens anteriores cuando corresponde.
Un JWT previo a V8 exige iniciar sesion otra vez; no intentar convertirlo desde el cliente.

## 4 Cambiar password propio

Pantalla comun para todos los roles. Campos:

- Password actual.
- Password nuevo.
- Confirmacion del nuevo.

```json
{
 "passwordActual":"valor-actual",
 "passwordNueva":"valor-nuevo-elegido-por-el-usuario",
 "confirmacionPassword":"valor-nuevo-elegido-por-el-usuario"
}
```

Los textos del ejemplo no son credenciales para usar. La politica real exige al menos 12
caracteres y como maximo 72 bytes UTF-8; usar TextEncoder para el limite de bytes.
Confirmacion debe coincidir y el nuevo debe diferir del actual. No hacer trim a passwords.
El backend verifica siempre el actual, tambien durante el primer acceso.

Exito: reemplazar TODA la sesion por el nuevo LoginResponse (token, usuario, expiracion,
memoria y sessionStorage). El token anterior deja de servir inmediatamente para peticiones nuevas.
Despues navegar al inicio autorizado del rol. No conservar el token previo con solo cambiar el flag.
Limpiar los tres campos y no guardarlos en storage, URL ni logs.
Un cambio voluntario posterior usa la misma pantalla y API, con requiereCambioPassword=false.

Error de password actual, confirmacion, longitud o reutilizacion: 400, mantener la sesion
y mostrar detail; no hacer logout por ese 400.

## 5 Restriccion obligatoria

Todas las rutas privadas de modulos deben comprobar:

1. Hay sesion vigente; si no, ir a /login.
2. Si requiereCambioPassword=true, ir a /cuenta/cambiar-password.
3. Si no hay cambio pendiente, comprobar el rol permitido para esa ruta.

La ruta de cambio debe permitir tanto cambios obligatorios como voluntarios y no producir un
bucle de redireccion. No aplicarle el guard que exige password definitivo.
Mostrar en modo obligatorio un mensaje claro y la opcion Cerrar sesion, sin menu operativo.

El servidor responde asi si una cuenta pendiente intenta otra operacion:

```json
{
 "type":"about:blank",
 "title":"Cambio de password requerido",
 "status":403,
 "codigo":"CAMBIO_PASSWORD_REQUERIDO",
 "detail":"Debe cambiar su password inicial antes de operar"
}
```

El interceptor debe reconocer codigo=CAMBIO_PASSWORD_REQUERIDO y llevar a cambio de password,
sin tratarlo como password de login incorrecto ni cerrar la sesion por un 403 comun.
Un 403 sin ese codigo es falta de permiso. Un 401 indica sesion invalida/expirada/revocada:
cerrar esa sesion y volver a login.
Conservar el control ya existente para que un 401 tardio del token anterior no cierre la sesion nueva.

## 6 Crear usuarios desde Administrador

Se conserva POST /usuarios con {username,password,rol}. La cuenta nueva queda activa,
requiereCambioPassword=true y version=0. El servidor no devuelve password ni hash.
Puede crearse cualquiera de los cinco roles, incluido otro Administrador.
Mostrar que la clave es temporal y debe comunicarse por un medio privado al titular.
No exponer la clave despues de cerrar el formulario ni agregarla a la tabla.

## 7 Editar cuenta, desactivar y reactivar

El listado GET /usuarios conserva su formato array y agrega los dos campos nuevos.
Columnas sugeridas: usuario, rol, activo, cambio de password pendiente, acciones.
No mostrar passwords, hashes ni version como dato de negocio; version se conserva internamente.

PUT /usuarios/{id} requiere el objeto completo:

```json
{
 "username":"nuevo.usuario",
 "rol":"ALMACEN",
 "activo":false,
 "version":2,
 "motivo":"Baja temporal autorizada"
}
```

El id no se cambia. Los datos historicos siguen vinculados por FK.
La edicion invalida las sesiones previas de la cuenta aunque conserve el mismo rol.
Cambiar username obliga a usar ese nombre en el siguiente login.
Los estados activo false/true permiten desactivar/reactivar sin borrar historial.
Reactivar no restablece contrasenas, no elimina el cambio pendiente y no revive JWT antiguos.

Desactivar debe pedir confirmacion y motivo. No escribir una version inventada:
usar la del registro con el que se abrio el formulario.
Un 409 por version obsoleta pide recargar; no reenviar automaticamente con otra version.
Los nombres duplicados tambien producen 409; mostrar detail.

Para la propia cuenta del administrador (id igual al de la sesion), ocultar/deshabilitar
Editar, Desactivar y Restablecer. El backend devuelve 409 si se intenta por otra via.
El cambio propio de password sigue disponible en Mi cuenta. No hay edicion de username propio en ese modulo.

## 8 Restablecer password de otra cuenta

Dialogo exclusivo de Administrador: password temporal, confirmacion y motivo.

```json
{
 "passwordTemporal":"clave-temporal-elegida-por-el-administrador",
 "confirmacionPassword":"clave-temporal-elegida-por-el-administrador",
 "motivo":"Recuperacion de acceso solicitada por el titular",
 "version":2
}
```

Misma politica de longitud/bytes; debe diferir del password actual de esa cuenta.
Respuesta User con requiereCambioPassword=true y version actualizada, sin password ni token.
El Administrador conserva su propia sesion. La cuenta afectada pierde sus tokens anteriores
y debe entrar con la clave temporal y cambiarla antes de operar.
Restablecer no reactiva una cuenta inactiva. No hay envio automatico por correo.
Limpiar el formulario al finalizar. El listado solo muestra Pendiente de cambio, nunca la clave.
Para la propia clave del Administrador usar el cambio personal, no este endpoint.

## 9 Auditoria

Boton Historial en Usuarios: GET /usuarios/{id}/eventos.
Mostrar fecha (convertida para presentacion), actorUsername, tipo y detalle.
Tipos: INSTALACION, CREACION, ACTUALIZACION, CAMBIO_PASSWORD y RESTABLECIMIENTO_PASSWORD.
actorId=null y actorUsername=SISTEMA identifica inicializacion tecnica.
No hay acciones para editar o borrar eventos.

## 10 Archivos Angular a modificar

- core/auth/auth.models.ts: ampliar User con requiereCambioPassword y version.
- auth.service.ts: agregar me y cambiarPassword; compartir un metodo para aplicar LoginResponse
  tanto al login como al cambio. Nunca almacenar passwords.
- auth.interceptor.ts: tratar 403 con codigo especial, 401 vigente y 401 tardio del token anterior.
- auth.guards.ts: guard autenticado comun, redireccion de cambio obligatorio y guard de roles.
- app.routes.ts: agregar /cuenta/cambiar-password con acceso para los cinco roles;
  completar despues las areas /compras, /supervisor y /consulta.
- Layout/header: enlace Mi cuenta o Cambiar mi password y cierre de sesion.
- pages/admin/resources: habilitar edicion/activacion/desactivacion de usuarios, reset e historial,
  conservando el comportamiento independiente de productos y almacenes.
- Servicio HTTP: PUT de usuarios, POST de reset, GET de eventos y modelos correspondientes.

No dejar la condicion antigua kind() !== 'usuarios' como veto absoluto de edicion.
La edicion de usuarios tiene payload/validaciones propias: no reutilizar sin adaptar los campos
de creacion (password y confirmacion no van en PUT).

## 11 Pruebas que debe realizar el frontend

1. Cada rol con cambio pendiente llega al formulario comun, tambien con URL directa.
2. No se envian peticiones de panel/listados antes de completar ese cambio.
3. Password incorrecto/mismatch/mas de 72 bytes muestra error sin perder sesion.
4. Exito reemplaza token y habilita solo los permisos del rol.
5. Cambio voluntario posterior funciona y un 401 viejo no elimina la sesion nueva.
6. Administrador crea cada rol; al entrar, la nueva cuenta debe cambiar la clave.
7. Editar con version correcta actualiza; version obsoleta exige recargar.
8. Desactivar bloquea login; reactivar permite login nuevo, no el token viejo.
9. Reset revoca las sesiones y vuelve a exigir cambio; no se presenta la clave en tablas/eventos.
10. La propia cuenta no se administra desde la tabla; otros roles no abren Usuarios.
11. Intentos directos indebidos reciben 403; no existe registro publico.
12. Validar E2E con backend y PostgreSQL de pruebas, no solo respuestas HTTP simuladas.

Las cuentas iniciales se provisionan en el backend al instalar; Angular no llama a un endpoint
publico para crearlas. Las credenciales iniciales pertenecen al responsable del entorno, no al codigo del cliente.

## 12 Texto listo para pasar al responsable Angular

> Integra en la rama front el backend StockGuard actualizado con Flyway V8. Lee primero
> backend/docs/APF3/cuentas/CONTRATO-FRONTEND.md y frontend/docs/ROLES-PERMISOS-Y-PENDIENTES.md.
> Implementa Mi cuenta y cambio obligatorio/voluntario para todos los roles, GET /auth/me,
> reemplazo completo de LoginResponse al cambiar password, guards sin bucles y los errores
> 401/403 con codigo CAMBIO_PASSWORD_REQUERIDO. Amplia Usuarios de Administrador con edicion,
> version/motivo, activacion/desactivacion, reset temporal e historial, protegiendo su propia cuenta.
> Conserva los permisos separados y completa las areas Compras, Supervisor y Consulta segun
> la matriz; Administrador no tiene permisos operativos universales. Nunca incluyas passwords,
> JWT_SECRET o credenciales PostgreSQL en Angular. Usa la API existente y los DTOs Java.
> Ejecuta los doce escenarios de cuentas y los flujos F01-F05 contra una base de pruebas,
> ademas de las pruebas/compilacion Angular. Documenta resultados reales y pendientes.
