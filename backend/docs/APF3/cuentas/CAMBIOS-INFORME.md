# Actualizar el informe por el ciclo de vida de cuentas

Esta ampliacion conserva los cinco roles y sus permisos de inventario/compras. No crea
SUPERADMIN ni cambia las formulas I05-I10, el problema, los objetivos o la linea base simulada.
Actualizar RF09 y su trazabilidad de forma concreta; no declararlo implementado en Angular
hasta completar el contrato y sus pruebas de integracion.

## Requisitos y actores

Complementar RF09: el sistema inicializa cinco cuentas en la primera instalacion con credenciales
privadas, exige cambio inicial de password, permite cambio propio y administra cuentas ajenas
mediante Administrador. Desactivacion/edicion/restablecimiento revocan sesiones previas.
La gestion incluye motivos, control de version, proteccion del ultimo administrador y auditoria.

Todos los roles acceden a Mi cuenta. Solo Administrador con password definitivo puede crear,
editar, activar/desactivar, asignar rol, restablecer claves temporales y consultar auditoria.
El administrador no se administra a si mismo desde Usuarios. No hay registro publico ni
recuperacion automatica por email. Las cuentas siguen teniendo un solo rol.

Agregar casos de uso: instalar cuentas, completar primer acceso, consultar Mi cuenta,
cambiar password propio, editar cuenta ajena, desactivar/reactivar, restablecer password temporal,
consultar auditoria y resolver conflicto de version.
Representar primer acceso y restablecimiento con requiereCambioPassword=true, no como un nuevo rol.

## Modelo y migraciones

V8 agrega a usuario:

- requiere_cambio_password (boolean).
- version_sesion (bigint), para revocar JWT anteriores.
- version (bigint con @Version), para evitar escrituras obsoletas.

Agrega evento_usuario: FK al usuario afectado, FK opcional al actor, nombres historicos,
tipo, fecha y detalle. No guarda passwords ni tokens en los eventos.
Agrega control_cuentas: una fila tecnica para marcar instalacion y serializar cambios criticos.
No es una tabla de roles.

El total pasa a 12 tablas de negocio (incluida evento_usuario), una tabla tecnica control_cuentas
y flyway_schema_history: 14 tablas en total. Se mapean 13 entidades JPA; Flyway no es una entidad.
Actualizar diagrama ER/logico/fisico y capas. Mantener V1-V7 sin reescribirlas.
En la migracion, las cuentas existentes conservan hashes, roles y estados. El cambio obligatorio
no se impone retroactivamente: para exigirlo se restablece una clave temporal desde Administracion.

## Trazabilidad y evidencia

Actualizar API/diagramas con /auth/me, /auth/cambiar-password, PUT /usuarios/{id},
POST /usuarios/{id}/restablecer-password y GET /usuarios/{id}/eventos.
La respuesta de usuario agrega requiereCambioPassword y version. Login y cambio propio
devuelven el mismo formato de sesion; el cambio propio emite un token nuevo.

En I02 ampliar la matriz de contratos de autorizacion. Las rutas antiguas no cambian de rol,
pero hay cinco contratos nuevos; recalcular el denominador a partir del reporte generado.
Registrar pruebas de cuentas como complemento, separadas de las ocho reglas operativas RN01-RN08.
I01/I03 siguen requiriendo evidencias de la interfaz integrada: las pruebas backend no reemplazan
la implementacion de pantallas, guards e interceptor Angular.

Evidencias a incorporar al Capitulo III cuando Angular este integrado:

1. Primer login de cada rol que muestra cambio obligatorio.
2. Intento directo de consultar productos antes del cambio: 403 con codigo especial.
3. Cambio correcto que habilita el panel del rol y revoca el token previo.
4. Creacion de una cuenta por Administrador y primer acceso de esa cuenta.
5. Edicion con motivo y version; conflicto 409 cuando se reutiliza una version vieja.
6. Desactivacion y rechazo de login; reactivacion y necesidad de login nuevo.
7. Restablecimiento temporal y nuevo cambio obligatorio.
8. Historial del usuario sin passwords, hashes ni tokens.
9. Rechazo de un rol no autorizado y proteccion de la propia cuenta del administrador.
10. Version de fuentes, motor, cantidad de pruebas y resultados reales del reporte.

## Instalacion y despliegue

Sustituir la explicacion de "solo se crea el primer administrador" por la instalacion
atomica de cinco cuentas con passwords privados distintos. No presentar local,demo como
requisito para operar: ese perfil queda reservado a datos/cuentas de demostracion adicionales.
En produccion las credenciales iniciales se suministran por secretos del alojamiento.
Explicar que el login verifica el hash en la base, no el archivo de configuracion.

Se escribio y ejecuto una primera prueba de cuenta antes de implementar el endpoint:
fallo con 403 donde se esperaba 200. Los demas escenarios completan la regresion posterior.
No afirmar que todas las pruebas se escribieron antes del codigo.
