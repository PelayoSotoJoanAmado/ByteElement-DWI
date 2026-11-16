> Estado vigente: las cinco areas estan implementadas y se ejecutaron 20 grupos de recorridos
> reales con backend/PostgreSQL. La matriz se conserva. Ver [revision integrada](../../backend/docs/APF3/revision-integrada/README.md).
> El diagnostico que sigue es historico y permite trazar los pendientes resueltos.

# Roles, permisos y pendientes de integracion

Revision del 16 de noviembre de 2026, rama front, commit observado 36791b2.
Se revisaron rutas, guards, menus, componentes y contratos del backend. En la revision inicial
no se modifico codigo ni se probaron las pantallas. Posteriormente se amplio el backend de cuentas:
su validacion esta en [cuentas/VALIDACION.md](../../backend/docs/APF3/cuentas/VALIDACION.md).
No se modificaron ni validaron los formularios Angular con esta ampliacion de backend.
La presencia de una pantalla no equivale a que su flujo E2E este validado.

## Conclusion

El backend define cinco roles. Angular reconoce los cinco nombres de rol, pero solo implementa
areas privadas para ADMINISTRADOR y ALMACEN. COMPRAS, SUPERVISOR y CONSULTA_COMERCIAL no tienen
todavia un area funcional propia o compartida a la que puedan acceder.

Por tanto, el problema principal es la integracion incompleta de tres roles. No se encontro en
los componentes revisados una necesidad de dar todas las operaciones al administrador.
Administrar usuarios/catalogo y autorizar operaciones de inventario/compras son responsabilidades distintas.

## Responsabilidad de cada rol

| Rol | Responsabilidad | Acciones propias | Situacion en Angular |
|---|---|---|---|
| ADMINISTRADOR | Administracion del sistema y consulta | Gestionar cuentas ajenas y su auditoria; mantener productos y almacenes; consultar indicadores, evidencias y salud | Area /admin implementada en codigo; ampliar cuentas segun V8; compras son consultas |
| ALMACEN | Ejecucion fisica y registro de inventario | Entradas, salidas, transferencias, registrar conteos y recibir ordenes | Area /almacen implementada en codigo; falta verificar E2E real |
| COMPRAS | Preparar y tramitar reposicion | Enviar solicitudes con monto/urgencia, revisar similares y emitir orden de una solicitud aprobada | Sin area funcional |
| SUPERVISOR | Control y autorizacion | Configurar min/max; aprobar/rechazar; confirmar duplicidad; cancelar pendientes; confirmar ajustes; generar cortes; consultar indicadores | Sin area funcional |
| CONSULTA_COMERCIAL | Consulta de disponibilidad e historial | Consultar catalogo, saldo, movimientos y reportes permitidos | Sin area funcional |

ADMINISTRADOR no es un superusuario que ejecute cualquier operacion. ALMACEN registra un
conteo, pero el SUPERVISOR confirma su ajuste. COMPRAS emite la orden y ALMACEN registra su
recepcion. SUPERVISOR aprueba la solicitud, pero no emite la orden.
Una cuenta tiene un rol en el modelo actual; no hay multirrol ni seleccion de un rol operativo
diferente al contenido en el JWT. El Administrador puede editar el rol de otra cuenta con control de version y auditoria; no existe multirrol.

## Cuentas y primer acceso actualizados en V8

El backend ahora crea cinco cuentas durante la primera instalacion con cinco passwords privados
distintos y exige su cambio antes de operar. No requiere local,demo. Las bases existentes conservan
sus cuentas y passwords, sin renombrarlas ni reinicializarlas. No se crea un rol SUPERADMIN.
Todos los usuarios pueden cambiar su propio password. Administrador puede administrar cuentas
ajenas, activar/desactivar, restablecer claves temporales y consultar eventos de auditoria.
No puede administrar su propia cuenta desde Usuarios; utiliza Mi cuenta para su password.

Estas funciones de backend requieren ampliar el frontend. Contrato vigente:
[CONTRATO-FRONTEND.md](../../backend/docs/APF3/cuentas/CONTRATO-FRONTEND.md).
Configuracion de primera instalacion y despliegue:
[cuentas/README.md](../../backend/docs/APF3/cuentas/README.md).
Los textos APF3 anteriores sobre un unico administrador automatico y ausencia de edicion de usuarios
son historicos y quedan reemplazados por esos documentos.

Flujo de exposicion: Administrador administra cuentas/catalogo; Supervisor configura politicas;
Compras solicita; Supervisor decide; Compras emite; Almacen recibe; Consulta revisa disponibilidad.
Cada cuenta inicial debe cambiar su password antes de participar. Los roles operativos no cambian.

## Detalle de la matriz vigente del servidor

Leyenda: Si = permitido; - = no permitido. Esta matriz describe la politica actualmente implementada,
no propone ampliar permisos.

| Operacion | Admin | Almacen | Compras | Supervisor | Consulta comercial |
|---|:---:|:---:|:---:|:---:|:---:|
| Consultar productos, almacenes, politicas y saldo | Si | Si | Si | Si | Si |
| Consultar movimientos y reportes de rotacion/stock critico | Si | Si | Si | Si | Si |
| Crear/editar/desactivar productos y almacenes | Si | - | - | - | - |
| Crear/listar/editar cuentas ajenas, activar/desactivar, restablecer y auditar | Si | - | - | - | - |
| Mi cuenta y cambiar password propio | Si | Si | Si | Si | Si |
| Configurar politicas min/max | - | - | - | Si | - |
| Registrar entradas/salidas/transferencias | - | Si | - | - | - |
| Registrar conteos fisicos | - | Si | - | - | - |
| Consultar conteos | - | Si | - | Si | - |
| Confirmar conteo y ajuste | - | - | - | Si | - |
| Consultar reposicion, solicitudes, ordenes y recepciones | Si | Si | Si | Si | - |
| Enviar solicitudes de compra | - | - | Si | - | - |
| Aprobar/rechazar solicitudes | - | - | - | Si | - |
| Confirmar duplicidad de solicitudes | - | - | - | Si | - |
| Emitir orden interna | - | - | Si | - | - |
| Registrar recepciones parciales/completas | - | Si | - | - | - |
| Cancelar pendiente de una orden | - | - | - | Si | - |
| Generar corte de stock | - | - | - | Si | - |
| Consultar cortes e indicadores I05-I10 | Si | - | - | Si | - |
| Consultar evidencias tecnicas | Si | - | - | - | - |
| Consultar salud del servicio | Si | - | - | Si | - |

Fuente: [SeguridadConfig.java](../../backend/src/main/java/com/byteelement/stockguard/seguridad/SeguridadConfig.java).
ComprasService agrega validaciones de estado, monto, actor y concurrencia; tener el rol no
garantiza que cualquier solicitud sea valida. Las rutas y botones Angular deben reflejar esas reglas.

## Criterio para la matriz de interfaces

La matriz de interfaces que sigue se basa en la politica vigente y no cambia permisos.
Si el equipo elige un Administrador operativo, debe aprobar y aplicar tambien el cambio
descrito en la seccion Administrador con todas las operaciones antes de implementar esa variante.

## Matriz exacta de pestanas para implementar

Leyenda: C = consultar; accion entre parentesis = botones adicionales permitidos;
- = no mostrar pestana ni permitir acceso por URL.
Inicio siempre muestra un resumen permitido para ese rol: no todos pueden llamar a /dashboard.
Las filas representan interfaces compartibles; no es necesario duplicar sus componentes por rol.

| Pestana | Administrador | Almacen | Compras | Supervisor | Consulta comercial |
|---|---|---|---|---|---|
| Inicio | Resumen administrativo | Resumen operativo | Resumen de solicitudes/ordenes | Resumen de control | Resumen de disponibilidad |
| Usuarios | C + crear/editar cuentas ajenas, activar/desactivar, restablecer y auditar | - | - | - | - |
| Mi cuenta | C + cambiar password propio | C + cambiar password propio | C + cambiar password propio | C + cambiar password propio | C + cambiar password propio |
| Productos | C + crear/editar/desactivar | C | C | C | C |
| Almacenes | C + crear/editar/desactivar | C | C | C | C |
| Existencias | C | C | C | C | C |
| Movimientos | C historial | C + registrar entrada/salida | C historial | C historial | C historial |
| Transferencias | - | Registrar transferencia | - | - | - |
| Conteos fisicos | - | C + registrar conteo | - | C + confirmar ajuste | - |
| Reposicion | C | C | C + preparar solicitud | C | - |
| Solicitudes de compra | C | C | C + enviar solicitud | C + aprobar/rechazar/confirmar duplicidad | - |
| Ordenes de compra | C | C | C + emitir desde solicitud aprobada | C + cancelar pendiente | - |
| Recepciones | C | C + registrar recepcion | C | C | - |
| Politicas min/max | - | - | - | C + guardar politica por par | - |
| Cortes de stock | C lotes conocidos | - | - | C + generar corte | - |
| Reportes de inventario | C | C | C | C | C |
| Indicadores I05-I10 | C | - | - | C | - |
| Evidencias tecnicas | C | - | - | - | - |
| Estado del sistema | C | - | - | C | - |

Precisiones para que los botones no sean ambiguos:

- Reposicion: Preparar solicitud en Compras navega al formulario de Solicitudes con datos
  sugeridos. Consultar la sugerencia no crea ni envia una solicitud.
- Ordenes: Emitir orden se muestra en la solicitud APROBADA y tambien puede enlazarse desde
  la pestana Ordenes. No se permite crear una orden independiente de una solicitud aprobada.
- Recepciones: puede ser una pestana propia o una subpestana del detalle de Ordenes. Solo
  ALMACEN ve el formulario de registro; los otros roles indicados solo consultan su historial.
- Transferencias: el historial se consulta en Movimientos; no inventar un listado GET de
  transferencias. Si se incorpora una vista dedicada de consulta, reutilizar movimientos permitidos.
- Politicas: todos los roles pueden consultar el GET de politica de un par si se presenta
  en un detalle de catalogo. La pestana de configuracion se reserva al Supervisor.
- Cortes: el backend consulta un lote por su identificador y genera cortes, pero no tiene
  un endpoint para listar todos los lotes. No prometer un historial completo de lotes sin ampliar API.
- La administracion de cuentas V8 debe implementarse segun el contrato de primer acceso. No hay reactivacion de catalogo ni eliminacion fisica en la API actual.
- C no habilita exportaciones, ediciones ni acciones adicionales no incluidas en el contrato.

### Rutas de entrada y menus

| Rol | Entrada | Menu que debe mostrar |
|---|---|---|
| ADMINISTRADOR | /admin existente | Todas las filas con permiso en su columna |
| ALMACEN | /almacen existente | Todas las filas con permiso en su columna |
| COMPRAS | /compras propuesta | Todas las filas con permiso en su columna |
| SUPERVISOR | /supervisor propuesta | Todas las filas con permiso en su columna |
| CONSULTA_COMERCIAL | /consulta propuesta | Todas las filas con permiso en su columna |

Rutas hijas sugeridas: productos, almacenes, existencias, movimientos, transferencias,
conteos, reposicion, solicitudes, ordenes, recepciones, politicas, cortes, reportes, indicadores,
evidencias y estado. Solo crear bajo cada area las que le correspondan.
Usuarios incluye usuarios/nuevo solo para Administrador.
Las rutas actuales /admin/compras y /almacen/compras pueden conservarse como contenedores con
subpestanas Reposicion/Solicitudes/Ordenes; no es obligatorio renombrarlas.
Compartir componente no implica compartir permiso: un recurso puede ser editable para
Administrador y de consulta para Compras/Almacen/Supervisor/Consulta.

## Administrador con todas las operaciones

Si, es posible. El nombre ADMINISTRADOR no activa automaticamente esa capacidad en Spring.
En este proyecto el rol tiene una lista explicita de permisos y no hereda los de otros roles.
Actualmente un administrador puede consultar compras, pero no enviar solicitudes, aprobar,
emitir ordenes ni registrar recepciones.

Hay dos politicas posibles:

| Politica | Consecuencia | Recomendacion para esta entrega |
|---|---|---|
| Administrador de sistema y consulta, politica vigente | Mantiene las responsabilidades operativas en sus roles actuales | Recomendada para completar Angular sin cambiar reglas ya acordadas |
| Administrador con todas las operaciones | El mismo rol puede acceder a cada interfaz y accion, sujeto a las reglas del negocio | Valida si el equipo desea y documenta esa excepcion; requiere cambios coordinados |

La segunda opcion no permite aprobar su propia solicitud, recibir mas de lo ordenado,
dejar saldo negativo, repetir una decision ni saltarse justificaciones por monto.
Puede ejecutar cualquier clase de operacion cuando sus condiciones se cumplan, no anularlas.
Un administrador que envia una solicitud necesitara otro usuario autorizado para aprobarla.

Para esa segunda opcion:

1. Dar acceso operativo al ADMINISTRADOR tanto en SeguridadConfig como en los @PreAuthorize
   de ComprasService. Cambiar solo uno de los dos no es suficiente.
2. Permitirle consultar conteos y el resto de recursos que hoy le faltan, ademas de operar.
3. Agregar al menu admin todas las pestanas de la matriz y las acciones de cada rol operativo.
   Los guards de las areas deben seguir impidiendo el acceso de roles no autorizados.
4. Mantener las validaciones de negocio y la auditoria con el usuario real; no simular otro rol.
5. Actualizar pruebas positivas/negativas, matriz RBAC, casos de uso, informe y evidencias.
6. Conservar y probar tambien Compras, Supervisor, Almacen y Consulta: el administrador
   ampliado no reemplaza los cinco roles comprometidos.

No hace falta otro rol ni otra tabla si solo se amplian los permisos de ADMINISTRADOR.
Asignar varios roles por usuario seria un cambio distinto del modelo y no es necesario para esta opcion.
El presente documento especifica el estado vigente; ninguna de esas ampliaciones se aplico al codigo.

## Hallazgos concretos de la revision

1. [app.routes.ts](../src/app/app.routes.ts) solo declara /admin y /almacen como areas privadas.
2. [auth.guards.ts](../src/app/core/auth/auth.guards.ts), landingPath(), envia ADMINISTRADOR
   a /admin, ALMACEN a /almacen y los otros tres roles a /. No es un fallo de password:
   puede haber login correcto y despues aparecer la portada.
3. [admin-layout.ts](../src/app/pages/admin/layout/admin-layout.ts) solo distingue almacen o
   administrador para menu, titulo y ruta inicial. Agregar rutas para los otros roles sin
   generalizar esa seleccion podria mostrarles un menu/rotulo incorrecto.
4. AdminResources restringe mantenimiento al administrador; compartir esos catalogos como
   solo lectura en Almacen es coherente con el backend.
5. AdminQueries implementa consultas de compras. El texto que remite aprobaciones/emision
   a otros roles es correcto, pero sus interfaces todavia no existen.
6. WarehouseOperations registra movimientos, transferencias, conteos y recepciones.
   No debe agregarse alli aprobacion de compras ni confirmacion del ajuste.
7. No se encontraron formularios/acciones de envio de solicitudes, aprobacion/rechazo,
   confirmacion de duplicidad, emision, cancelacion, configuracion de politicas,
   confirmacion de conteos ni generacion de cortes.
8. Hay una discrepancia estatica en admin-access.spec.ts: una prueba espera que ALMACEN,
   al solicitar /admin, termine en /almacen; adminGuard actualmente devuelve /login con
   motivo sin-permiso. Revisar la politica de redireccion y ajustar implementacion/prueba.
   Esta observacion no constituye un resultado de ejecucion de la suite.

## Pantallas y acciones pendientes

No son necesarios cinco layouts copiados. Se pueden reutilizar tablas, formularios, consultas,
estructura visual y componentes, con permisos por accion y rutas accesibles a cada rol.
Las siguientes rutas son propuestas, no rutas que existan hoy:

### Compras

- /compras: inicio y solicitudes pendientes de decision/emision.
- Reposicion con consumo, pedidos pendientes, regla y cantidad sugerida.
- Nueva solicitud con producto/almacen, cantidad, monto, motivo, urgencia y clave idempotente.
- Advertencia de solicitudes similares; consultar su detalle.
- Solicitudes aprobadas: emitir orden una sola vez; historial y detalle de ordenes/recepciones.
- Catalogo, saldo y reportes de consulta reutilizados.

### Supervisor

- /supervisor: inicio de control e indicadores.
- Politicas min/max por producto/almacen.
- Bandeja de solicitudes: aprobar/rechazar, justificar montos que superen el umbral y confirmar duplicidad.
- Ordenes: consultar y cancelar el pendiente con motivo.
- Conteos: comparar fisico/saldo al corte y confirmar ajuste; mostrar conflicto si quedo obsoleto.
- Cortes: generar fotografia de stock y consultar el lote.
- Dashboard I05-I10 con periodo, numeradores, denominadores, unidad y estados sin datos.
- Catalogo, saldo, reposicion y reportes reutilizados como consulta.

### Consulta comercial

- /consulta: inicio de consulta, catalogo, disponibilidad por producto/almacen y movimientos/reportes permitidos.
- Sin mantenimiento ni operaciones de compra/inventario, sin bandejas de aprobacion.
- No asignarle indicadores operativos ni compras internas si se conserva la matriz actual.

Ademas: landingPath para cinco roles, guards para las rutas nuevas, menu/rotulo por rol,
acciones condicionadas y tratamiento coherente de 401/403. Ocultar botones no reemplaza
la autorizacion del backend; tampoco se debe usar una cuenta de administrador para todos.

## Por que se interrumpen los flujos

F03 necesita COMPRAS envia -> SUPERVISOR aprueba -> COMPRAS emite -> ALMACEN recibe.
Hoy Almacen tiene recepcion, pero no se puede recorrer desde Angular todo lo anterior.
F04 permite registrar el conteo, pero falta la confirmacion por Supervisor.
Sin interfaz de politicas/cortes, F05 puede depender de datos sembrados o acciones manuales
en Postman. Un indicador sin corte puede mostrar Sin datos aunque su calculo en el backend funcione.
F01/F02 tienen codigo de interfaz en Almacen; no se declaran aprobados E2E por esta revision.

## Efecto sobre el informe

Si se completan los roles conservando esta politica, no cambian el problema, objetivos, variables,
formulas I05-I10 ni el modelo relacional por el solo hecho de agregar pantallas.
No reducir el alcance comprometido a dos roles para justificar la interfaz actual.

| Seccion | Actualizacion necesaria |
|---|---|
| Actores y permisos | Una sola matriz coherente con el servidor; separar administrar, consultar, solicitar, aprobar, emitir y recibir |
| Casos de uso y actividades | Responsables correctos de cada paso y transiciones entre roles |
| Requisitos RF01/RF03-RF05/RF07-RF10 | Distinguir backend implementado, pantalla implementada y flujo integrado probado |
| Matriz de trazabilidad | Agregar componente/ruta Angular, rol, endpoint, prueba positiva/negativa y evidencia |
| Arquitectura/componentes | Agregar areas de Compras, Supervisor y Consulta y componentes compartidos; no cambian las tablas por ello |
| Capitulo III | Capturas y demostraciones con los cinco roles, incluyendo rechazos por permisos |
| Resultados/indicadores | No presentar pruebas del backend como prueba de los cinco flujos completos en Angular |

RF01 sigue necesitando la interfaz de politicas del Supervisor. RF03 tiene consultas en las dos
areas existentes, pero falta conectarlas al envio de Compras. RF04/RF05 carecen de las acciones
de Compras/Supervisor. RF07 esta incompleto en interfaz por la confirmacion del ajuste.
RF08 tiene consulta para Admin, pero falta el acceso del Supervisor y sus operaciones de soporte.
RF09 no se cierra solo por reconocer cinco nombres o impedir entrar al panel Admin:
cada rol necesita llegar a sus funciones permitidas. RF10 requiere los recorridos integrados.

Una redaccion fiel al estado revisado:

> El backend implementa autenticacion y autorizacion para cinco roles. El frontend incorpora
> las areas de Administrador y Almacen; la integracion de Compras, Supervisor y Consulta
> Comercial permanece pendiente. Por ello, los flujos de reposicion y compra, confirmacion
> de ajustes y control de cortes aun no se demuestran completos desde la interfaz.

Cuando esos modulos se implementen y prueben, reemplazar esa descripcion y anexar evidencias
de la version final. Si se decide cambiar quien puede aprobar, emitir o ajustar, entonces si
deben modificarse politica, matriz, backend, frontend y pruebas en conjunto.

## Criterio de cierre

- Los cinco roles inician sesion y aterrizan en un area autorizada.
- El acceso directo por URL respeta permisos y cada menu muestra sus consultas/acciones correctas.
- F03 y F04 se recorren con cambios de usuario reales y persistencia, sin Postman para completar pasos.
- Supervisor configura politicas/cortes y consulta los seis indicadores.
- Cada operacion se prueba con rol permitido y rol indebido; el servidor devuelve 403 al indebido.
- Los estados del informe y las evidencias corresponden a esa misma version.

Esta revision deja una especificacion y un diagnostico. No implementa los tres modulos faltantes.
