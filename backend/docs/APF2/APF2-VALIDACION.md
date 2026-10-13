# Validación del mínimo APF2

Fecha: 10 de octubre de 2026 (America/Lima). Actualización de la rama `prueba`, desde el commit `2175b10b0e66589134af80a07055b23ca0619cdb`, para relacionar usuarios con operaciones.

Comando ejecutado con Java 21.0.12: `./mvnw verify` (dependencias previamente descargadas; ejecución de verificación en modo offline).

Resultado: **BUILD SUCCESS, 58 pruebas, 0 fallos, 0 errores, 0 omitidas**. Se generó el JAR ejecutable de Spring Boot. `git diff --check` no presentó errores de espacios.

| Suite | Pruebas |
|---|---:|
| StockguardApplicationTests | 1 |
| ProductoServiceTest | 5 |
| AlmacenServiceTest | 5 |
| PoliticaStockServiceTest | 4 |
| InventarioControllerTest | 13 |
| CatalogoControllerTest | 6 |
| Apf2IntegrationTest | 21 |
| DatosDemoIntegrationTest | 1 |
| UsuarioMigrationTest | 2 |
| Total | 58 |

## Evidencia cubierta

- Flyway aplica V1, V2, V3 y V4 a una base nueva; Hibernate valida todas las entidades.
- Saldo y movimiento guardados se recuperan mediante JPA y SQL.
- Transferencia conserva ambos saldos y comparte referencia entre entrada/salida.
- Fallo provocado al construir el segundo movimiento revierte saldo y primer movimiento ya guardados.
- Stock insuficiente y sobreflujo numérico no alteran saldos.
- 12 salidas concurrentes contra saldo 5: exactamente 5 operaciones aceptadas, saldo final 0.
- 8 entradas concurrentes a un par sin existencia previa: un único registro, saldo 8.
- 8 transferencias concurrentes en ambos sentidos: suma y saldos iniciales conservados.
- JPQL filtra por SKU, almacén, período y pagina resultados.
- I05/I10 cuentan una sola vez un producto afectado en varios almacenes; editar después una política no cambia el corte.
- I07 conserva el saldo al corte, calcula 50 % para un conjunto conocido y distingue ausencia de observaciones.
- Ajustes trazables; doble confirmación y conteos obsoletos rechazados aunque el saldo vuelva al valor inicial.
- Login HTTP real vía MockMvc; ausencia de JWT, token inválido y expirado rechazados.
- Matriz de rechazo sin token, con JWT inválido y con rol indebido para 17 contratos restringidos. Accesos permitidos comprobados en pruebas de catálogo, políticas, movimientos y usuarios; los servicios y perfiles ejercitan conteos y cortes.
- El perfil demo arranca, genera datos conocidos y obtiene I05=100 %, I07=50 % e I10=50 %. Reejecutar su inicializador no duplica los datos.

## Relaciones con usuario

- Movimiento, conteo y corte guardan la FK del usuario autenticado; el conteo confirmado guarda además la FK del supervisor. Ninguna identidad se recibe del cliente.
- La base rechaza usuario inexistente, autor nulo y borrado de usuarios con operaciones asociadas.
- Cambiar el username no altera la FK ni el nombre histórico registrado en un movimiento.
- Un actor sin cuenta persistida no puede dejar movimientos ni saldos nuevos.
- Una base en V3 con movimientos, conteos confirmados/pendientes y cortes se actualiza a V4 conservando saldos, motivos y referencias. El backfill también enlaza usuarios desactivados.
- Si un autor histórico no tiene correspondencia, la migración se detiene conservando el registro y sin crear cuentas ficticias.

## Alcance de la verificación

Las pruebas de integración utilizan **H2 2.4 en modo PostgreSQL**, JPA/Flyway reales y la cadena de Spring Security. La aplicación no añade H2 como motor de producción. Este resultado no sustituye una ejecución de concurrencia y migraciones sobre PostgreSQL real.

No se ejecutó aquí el runner de Postman ni una restauración de backup. La colección se entrega para la demostración HTTP con PostgreSQL. La prueba de reinicio ejercita la idempotencia del inicializador, no un reinicio físico del servidor; la guía incluye el procedimiento para comprobar persistencia entre reinicios reales.

Para validación PostgreSQL, definir TEST_DB_URL/TEST_DB_USER/TEST_DB_PASSWORD hacia una base vacía de pruebas. La suite del perfil demo admite DEMO_TEST_DB_URL hacia una **segunda base vacía**, con las mismas credenciales; si no se define, esa prueba sigue usando H2. Los datos creados por pruebas no se borran de una base externa.
