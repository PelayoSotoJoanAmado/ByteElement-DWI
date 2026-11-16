# ByteElement-DWI - StockGuard MYPE

Para clonar y arrancar por primera vez, seguir [GUIA-INICIO.md](GUIA-INICIO.md).
Para conocer cada función y recorrer el sistema, consultar [GUIA-USO-SISTEMA.md](GUIA-USO-SISTEMA.md).
Para explicar las fórmulas, la medición y su relación con el informe, consultar [GUIA-INDICADORES-Y-TRAZABILIDAD.md](GUIA-INDICADORES-Y-TRAZABILIDAD.md).

Ultima correccion: [selectores de almacenes](frontend/docs/SELECTORES-ALMACENES.md), activos por defecto
y consulta historica opcional. 102 pruebas Angular, build y siete comprobaciones reales aprobados.

Revision integrada local: [flujo, base existente y resultados](backend/docs/APF3/revision-integrada/README.md).
94 pruebas backend por motor, 97 Angular y 20 grupos de navegador aprobados.
Node actualizado a 26.13.1: 97 pruebas Angular y build comprobados de nuevo; la base existente se conserva al migrar a V8.

Actualizacion de cuentas (16/11): [instalacion normal de cinco roles](backend/docs/APF3/cuentas/README.md),
[contrato detallado para Angular](backend/docs/APF3/cuentas/CONTRATO-FRONTEND.md) y
[cambios al informe](backend/docs/APF3/cuentas/CAMBIOS-INFORME.md).
Las cuentas nuevas deben cambiar su password antes de operar; Angular ya implementa ese flujo.

Backend Java 21 / Spring Boot / JPA-Hibernate / PostgreSQL para inventario y reposicion.
APF3 completa compras, recepciones, reglas de reposicion, I05-I10, evidencias de backend y CORS para Angular.
La carpeta `frontend/` contiene la base Angular con SCSS, la presentación pública, el inicio de sesión y el panel de administrador protegido de StockGuard.
Las cinco areas de rol estan integradas; los recorridos locales verificados y sus limites se documentan en la revision integrada.

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
Las migraciones V1-V8 se aplican con Flyway; Hibernate valida el esquema.
InsForge puede incorporarse despues como PostgreSQL por JDBC, conservando el backend.

El trabajo actual de integracion esta en la rama front. Las evidencias APF1/APF2 son historicas y no describen el alcance actual.

## Frontend Angular

Desde `frontend/`, con Node compatible con Angular 22 (ver `engines` en package.json):

```bash
npm ci
npm start
```

Abrir http://localhost:4200. Para compilar: `npm run build`. Para verificar: `npm test -- --watch=false`.
Consulta [la guía del frontend](frontend/README.md) para conocer el alcance de esta entrega.
