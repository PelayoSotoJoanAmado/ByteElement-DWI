# Validacion de cuentas y primer acceso

Corte historico de backend. La [revision integrada posterior](../revision-integrada/README.md)
amplia la suite a 94 pruebas por motor y ejecuta 20 grupos reales desde Angular; consultar su evidencia vigente.

Fecha: 16 de noviembre de 2026, America/Bogota. Rama front, base de trabajo 36791b2.
Las pruebas se ejecutaron antes de dividir la entrega en commits; por eso sus reportes identifican
esa base, cambios sin commit y el SHA-256 de las fuentes verificadas. Los commits posteriores
organizan la entrega sin alterar esas fuentes. Las evidencias no representan una interfaz Angular terminada.

## Resultados comprobados

| Verificacion | Resultado | Evidencia |
|---|---|---|
| Suite completa H2 modo PostgreSQL | 92 pruebas, 0 fallos/errores/omitidas | [resultado-h2.json](evidencias/resultado-h2.json) |
| Suite completa PostgreSQL 18.1 / Java 21.0.10 | 92 pruebas, 0 fallos/errores/omitidas | [resultado-postgresql.json](evidencias/resultado-postgresql.json) |
| Contratos protegidos | 49, incluidos cinco nuevos; roles permitidos/denegados y tokens invalidos | seguridad[] de ambos reportes |
| Controles de cuentas | Siete grupos aprobados de instalacion, primer acceso, revocacion, gestion, concurrencia y migracion | cuentas[] de ambos reportes |
| Empaquetado | Maven package correcto; JAR ejecutado con perfil prod, sin demo | Prueba HTTP siguiente; artefacto temporal en target |
| API real sobre PostgreSQL nuevo | Siete grupos aprobados, cinco cuentas iniciales, sin productos iniciales | [api.json](evidencias/api.json) |

En la ejecucion PostgreSQL se configuraron tres bases exclusivas: principal, demo e instalacion.
UsuarioMigrationTest y PerfilProduccionIntegrationTest conservan sus escenarios H2 incluso en
esa ejecucion. El primer escenario incluye V7 -> V8 con hashes, roles y estados previos conservados.
No se ejecuto PostgreSQL 16; el compose historico usa esa version.

## Escenarios de seguridad y consistencia

- Inicializacion atomica: faltan claves, hay nombres/claves duplicadas o falla auditoria ->
  no quedan cuentas parciales. Dos inicializadores concurrentes no crean duplicados.
- Reiniciar conserva passwords/estados; no repone cuentas borradas ni exige otra vez secretos
  iniciales en una instalacion que ya tiene usuarios.
- Los cinco roles pueden consultar Mi cuenta y cambiar su password inicial; el resto recibe
  403 con codigo CAMBIO_PASSWORD_REQUERIDO hasta completar el cambio.
- Clave actual incorrecta, confirmacion distinta, clave corta, reutilizacion y exceso de 72
  bytes UTF-8 se rechazan sin modificar la cuenta.
- Cambio propio/restablecimiento/edicion/desactivacion invalidan tokens anteriores.
  Reactivar no hace validos los JWT viejos; se necesita un login nuevo.
- Solo Administrador con password definitivo administra cuentas. Los demas roles no acceden
  a ese modulo aunque intenten invocar directamente los endpoints.
- Version obsoleta y nombres duplicados producen 409. Dos ediciones de la misma version
  no se pisan. Dos administradores no pueden desactivarse mutuamente dejando cero activos.
- Administrador no puede editarse/desactivarse/cambiar su rol ni restablecerse desde Usuarios.
- Los eventos registran actor/fecha/cambio/motivo sin incluir las credenciales de los payloads.

La prueba HTTP uso el JAR real en 127.0.0.1:18082, perfil prod y una base PostgreSQL nueva
aislada. Se comprobaron login/cambio/revocacion para cada rol y luego creacion, edicion,
conflicto de version, reset, desactivacion, reactivacion e historial de una cuenta adicional.
Tambien se completo solicitud por Compras, aprobacion por Supervisor, emision por Compras y
recepcion por Almacen; se rechazaron los intentos de Administrador de solicitar y de Compras
de aprobar. Consulta comercial no accedio a compras. Los permisos de negocio se conservaron.
El esquema HTTP quedo con ocho migraciones y 14 tablas publicas, incluido el historial Flyway.

## Repetir sin tocar datos del equipo

Desde backend, para H2, sin TEST_DB_URL/DEMO_TEST_DB_URL/INSTALACION_TEST_DB_URL heredados:

```powershell
.\docs\APF3\Verificar-APF3.ps1 -ArchivoResultado docs/APF3/cuentas/evidencias/resultado-h2.json
```

Para PostgreSQL configurar TEST_DB_URL, DEMO_TEST_DB_URL e INSTALACION_TEST_DB_URL hacia
tres bases de pruebas diferentes; TEST_DB_USER/TEST_DB_PASSWORD deben tener acceso a ellas.
El nombre de la tercera debe contener test o prueba. No usar una base operativa: la suite de
instalacion elimina exclusivamente sus propias cuentas inst-* entre casos para probar un inicio vacio.
Las pruebas principales y demo insertan sus propios datos. Despues ejecutar:

```powershell
.\docs\APF3\Verificar-APF3.ps1 -ArchivoResultado docs/APF3/cuentas/evidencias/resultado-postgresql.json
```

El verificador tambien conserva RF01-RF10, RN01-RN08, los contratos y los flujos HTTP de negocio.
El reporte general APF3/evidencias/resultado.json se actualizo con la misma ejecucion PostgreSQL.
Las evidencias del 13/11 de volumen/restauracion siguen siendo historicas: no se repitio el
benchmark de 10000 productos durante esta ampliacion.

No se arranco esta version contra la base local byteelement ni se modifico su archivo privado.
No se configuraron InsForge ni servicios cloud. Los procesos temporales de validacion se detienen
al cerrar el trabajo. Los artefactos y credenciales temporales permanecen fuera de Git en target.
La aceptacion Angular requiere los doce escenarios de [CONTRATO-FRONTEND.md](CONTRATO-FRONTEND.md)
y los flujos integrados F01-F05; no se atribuyen aqui resultados de navegador.
