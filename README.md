# ByteElement-DWI - StockGuard MYPE

Backend Java 21 / Spring Boot / JPA-Hibernate / PostgreSQL para inventario y reposicion.
APF3 completa compras, recepciones, reglas de reposicion, I05-I10, evidencias de backend y CORS para Angular.
La carpeta `frontend/` contiene la base Angular con SCSS, la presentación pública, el inicio de sesión y el panel de administrador protegido de StockGuard.
Los apartados del administrador están conectados a sus contratos de API: cuentas, catálogos, consultas, reportes, indicadores, salud y evidencias. La validación E2E de estos nuevos flujos con la base del usuario sigue pendiente; las áreas de los demás roles se desarrollan después.

- [Estado y traspaso](HANDOVER_ESTADO_PROYECTO.md)
- [Documentacion APF3](backend/docs/APF3/README.md)
- [Contrato para Angular](backend/docs/APF3/API-ANGULAR.md)
- [Cambios que deben trasladarse al informe](backend/docs/APF3/CAMBIOS-INFORME.md)
- [Arranque, volumen, backup e InsForge](backend/docs/APF3/OPERACION.md)
- [Coleccion Postman APF3](backend/docs/APF3/StockGuard-APF3.postman_collection.json)
- [Evidencia de pruebas](backend/docs/APF3/evidencias/resultado.json)

Desde backend: `./mvnw.cmd test` o `./docs/APF3/Verificar-APF3.ps1`.
Para arrancar: configurar secretos con application-local.properties.example y ejecutar
`./mvnw.cmd spring-boot:run`. El perfil local ya no activa demo automaticamente.
Prod no importa secretos locales; usar SPRING_PROFILES_ACTIVE=prod y variables de entorno.
Las migraciones V1-V7 se aplican con Flyway; Hibernate valida el esquema.
InsForge puede incorporarse despues como PostgreSQL por JDBC, conservando el backend.

Se mantiene la rama prueba. Las evidencias APF1/APF2 son historicas y no describen el alcance actual.

## Frontend Angular

Desde `frontend/`, con Node compatible con Angular 22 (ver `engines` en package.json):

```bash
npm ci
npm start
```

Abrir http://localhost:4200. Para compilar: `npm run build`. Para verificar: `npm test -- --watch=false`.
Consulta [la guía del frontend](frontend/README.md) para conocer el alcance de esta entrega.
