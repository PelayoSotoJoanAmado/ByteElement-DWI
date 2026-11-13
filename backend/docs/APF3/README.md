# Backend de StockGuard para APF3

Esta etapa completa los contratos de inventario y compras para que Angular pueda consumirlos.
InsForge es opcional: la aplicacion sigue usando PostgreSQL mediante JDBC, JPA/Hibernate y Flyway.
La evidencia de este directorio distingue backend probado de interfaz Angular y despliegue pendientes.
Las guias APF1/APF2 se conservan como historial; este documento reemplaza sus pendientes de compras.

## Documentos

- [Inicio para el responsable de Angular](INICIO-COMPANERO-ANGULAR.md): lectura, entorno, usuarios y orden de integracion.

- [Reglas y modelo](REGLAS-Y-MODELO.md): criterios implementados, indicadores y cambios relacionales.
- [Contrato para Angular](API-ANGULAR.md): rutas, permisos, cuerpos, fechas y errores.
- [Cambios para el informe](CAMBIOS-INFORME.md): secciones que debe corregir el equipo.
- [Operacion, volumen e InsForge](OPERACION.md): arranque, carga, medicion, backup y conexion futura.
- [Incorporar InsForge paso a paso](INCORPORAR-INSFORGE.md): conexion JDBC, base nueva, traslado y comprobacion.
- [Validacion realizada](VALIDACION.md): pruebas, datos, rendimiento y limites de la evidencia.
- [Resultado verificable](evidencias/resultado.json): generado por Verificar-APF3.ps1, nunca un porcentaje escrito a mano.

## Verificacion

Desde backend:

```powershell
.\docs\APF3\Verificar-APF3.ps1
```

La suite predeterminada usa H2. Para verificar PostgreSQL, definir TEST_DB_URL, TEST_DB_USER,
TEST_DB_PASSWORD y DEMO_TEST_DB_URL hacia dos bases de pruebas dedicadas antes de ejecutar.
Las pruebas agregan registros; las claves son unicas por escenario. No apuntar a datos de operacion.
UsuarioMigrationTest y PerfilProduccionIntegrationTest conservan escenarios H2 incluso al seleccionar PostgreSQL.
No publicar logs DEBUG: pueden contener cuerpos HTTP y credenciales de pruebas.

El reporte registra commit, cambios sin commit, huella SHA-256 de fuentes, motor, fecha y pruebas
por requisito/regla/flujo. El alcance I01-I04 de este reporte es backend. La aceptacion completa
del curso requiere comprobar F01-F05 desde Angular en el entorno desplegado.

I02 se obtiene de la matriz de 44 contratos protegidos: token ausente/invalido, cada rol permitido
y cada rol denegado. El caso de acceso permitido usa cuerpos incompletos o IDs inexistentes,
por lo que 400/404 prueban que la autorizacion permitio llegar al controlador. El funcionamiento
de negocio se verifica por separado en las pruebas de integracion y el flujo HTTP.
La coleccion StockGuard-APF3.postman_collection.json permite repetir una demostracion manual
con credenciales locales de Postman; no exportar esas credenciales al repositorio.

Los contratos nuevos no exigen registrar datos en InsForge ni modificar las migraciones V1-V5.
