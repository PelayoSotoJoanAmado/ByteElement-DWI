# Validacion del backend APF3

Fecha: 13 de noviembre de 2026, America/Bogota.
Se verificaron los contratos del backend; no se declara terminada la interfaz Angular ni el despliegue.
Las evidencias JSON conservan timestamps UTC y la huella de las fuentes.

## Pruebas

| Entorno | Resultado | Evidencia |
|---|---|---|
| H2 modo PostgreSQL | 71 pruebas, 0 fallos, 0 errores, 0 omitidas | evidencias/resultado-h2.json |
| PostgreSQL 18.1 / Java 21.0.10 | 71 pruebas, 0 fallos, 0 errores, 0 omitidas | Ejecucion local documentada; controles SQL/HTTP y restauracion en evidencias |
| Matriz de autorizacion | 44 contratos, todos los roles, token ausente/invalido | seguridad[] en resultado.json |
| API real de evidencias y preflight CORS Angular | Correctos | evidencias/api.json |
| Empaquetado ejecutable | Maven package correcto; JAR arrancado y consultado por HTTP | JAR temporal en target |

UsuarioMigrationTest y PerfilProduccionIntegrationTest usan H2 incluso en la ejecucion PostgreSQL:
el primero comprueba migraciones historicas y el segundo el perfil de configuracion.
No se ejecuto una instancia PostgreSQL 16; el compose existente conserva esa version.
resultado.json representa la ultima ejecucion del verificador y puede quedar reemplazado
por una ejecucion H2 posterior. Consultar siempre su campo motor; no atribuir a PostgreSQL
un archivo que indique H2. Para conservar ambos reportes usar ArchivoResultado con nombres distintos.

Las pruebas nuevas verifican idempotencia de envio, advertencia versus duplicidad confirmada,
condicion de monto, fechas de rechazo, urgencia, emision unica concurrente, recepcion parcial,
reintento de recepcion, exceso rechazado, recepciones concurrentes, cancelacion de pendiente,
formula de reposicion conocida, I06/I08/I09, ausencia de datos y flujo HTTP completo de compra.
Se conserva la cobertura anterior de movimientos, transferencias, conteos, ajustes y JWT.

La matriz I02 verifica acceso autorizado con solicitudes incompletas o IDs inexistentes:
400/404 demuestran que se alcanzo validacion/negocio, no que la operacion tuvo exito.
Las pruebas de negocio e integracion se registran por separado.
I01-I04 reportados como backend no sustituyen la aceptacion de los flujos desde Angular.

## Carga y medicion

La base exclusiva stockguard_volumen recibio 10000 productos y 164840 filas de negocio nuevas.
Tamano observado: aproximadamente 42 MB, incluyendo indices y tablas auxiliares.
Los seis indicadores se verificaron por HTTP contra valores conocidos:

| Indicador | Numerador | Denominador | Resultado |
|---|---:|---:|---:|
| I05 | 1000 productos | 10000 productos | 10% |
| I06 | 368 ordenes urgentes | 1840 ordenes emitidas | 20% |
| I07 | 900 coincidencias | 1000 conteos | 90% |
| I08 | 317952000 segundos | 1840 aprobaciones | 2 dias |
| I09 | 160 duplicadas confirmadas | 2000 solicitudes | 8% |
| I10 | 1000 productos | 10000 productos | 10% |

Estos resultados sinteticos son controles de calculo, no una mejora causal demostrada.
Detalle en evidencias/volumen.json.

Medicion HTTP local, secuencial, pagina de 20 registros:

| Consulta | Peticiones | Errores | Promedio | p95 |
|---|---:|---:|---:|---:|
| Reposicion | 30 | 0 | 122.5 ms | 140.7 ms |
| Rotacion | 20 | 0 | 139.5 ms | 169.4 ms |

El primer dashboard completo de la carga tardo aproximadamente 1.94 segundos.
No es un SLA ni una prueba de carga concurrente HTTP. La concurrencia transaccional se
prueba en la suite; RNF06 requiere acordar un umbral para el entorno que finalmente se despliegue.

## Recuperacion

Se exportaron y restauraron stockguard_volumen y stockguard_test en dos bases distintas,
usando pg_dump/pg_restore con transaccion unica. Las once tablas de negocio conservaron
conteos y huellas MD5 del contenido ordenado. La base de pruebas restaurada incluyo
30 registros de recepcion_compra. Ademas, los seis indicadores del volumen restaurado
coincidieron por HTTP. Ver evidencias/restauracion.json.

Los backups, bases temporales y credenciales aleatorias quedaron en target, ignorado por Git.
Los servidores temporales se detuvieron al terminar.

## Incidencia de configuracion corregida

El primer arranque HTTP importo el archivo local antiguo y Flyway amplio la base local existente
de V5 a V7. No se cargo alli el conjunto masivo ni se eliminaron registros.
Se movio la importacion al archivo de recursos exclusivo del perfil local y se agrego
PerfilProduccionIntegrationTest. Los arranques posteriores y la carga usaron una base aislada.

## Trabajo que corresponde a la integracion

Conectar Angular a API-ANGULAR.md, formularios/rutas/interceptor y manejo de errores.
Ejecutar F01-F05 desde navegador, actualizar evidencias de la version integrada y desplegar con HTTPS.
Trasladar CAMBIOS-INFORME.md al documento academico y anexar las capturas de esa ejecucion.
