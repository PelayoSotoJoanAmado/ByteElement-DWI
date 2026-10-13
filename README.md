# ByteElement-DWI — StockGuard MYPE

Backend Spring Boot 4.1.1 / Java 21 / PostgreSQL para inventarios.

La rama `prueba` desarrolla el mínimo APF2: catálogo persistente, inventario transaccional con claves foráneas de usuario, consultas JPQL, JWT y cinco roles, conteos/ajustes, cortes e indicadores I05/I07/I10. RF08 es parcial; compras y frontend quedan para siguientes avances.

- [Instalación, permisos, fórmulas y alcance APF2](backend/docs/APF2/APF2-MINIMO.md)
- [Colección Postman APF2](backend/docs/APF2/StockGuard-APF2.postman_collection.json)
- [Validación: 58 pruebas aprobadas](backend/docs/APF2/APF2-VALIDACION.md)
- [Modelo relacional](backend/docs/APF2/APF2-MODELO.md)

Desde `backend`: `./mvnw test`. Para iniciar: configurar credenciales con el archivo de ejemplo y ejecutar `./mvnw spring-boot:run`. Windows: `mvnw.cmd`.

Las evidencias APF1 se conservan como historial de la versión inicial, anterior a seguridad y persistencia del inventario.
