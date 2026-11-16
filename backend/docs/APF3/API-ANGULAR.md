# Contrato de API para Angular

Ampliacion vigente de autenticacion/cuentas: [cuentas/CONTRATO-FRONTEND.md](cuentas/CONTRATO-FRONTEND.md).
Incluye modelos completos, los cinco endpoints agregados, bloqueo por password pendiente,
revocacion y manejo de errores. Aplicar ese contrato junto con los contratos de negocio de esta pagina.

Base /api/v1; JSON; Authorization: Bearer accessToken. Login POST /auth/login devuelve
accessToken, tokenType, expiresIn y usuario (id, username, rol, activo, requiereCambioPassword, version). Tokens duran 30 minutos.
Guardar JWT solo para la sesion; enviar mediante interceptor. Los guards de Angular complementan
las restricciones del servidor. No enviar autor ni fechas para atribuir una mutacion.

## Nuevos contratos

| Metodo y ruta relativa | Rol | Entrada |
|---|---|---|
| GET /reposicion | ALMACEN, COMPRAS, SUPERVISOR, ADMINISTRADOR | sku?, almacenId?, pagina=0, tamanio=20 |
| POST /solicitudes-compra | COMPRAS | CrearSolicitud |
| GET /solicitudes-compra y /solicitudes-compra/{id} | ALMACEN, COMPRAS, SUPERVISOR, ADMINISTRADOR | filtros o id |
| POST /solicitudes-compra/{id}/aprobar | SUPERVISOR | {justificacion: string o null} |
| POST /solicitudes-compra/{id}/rechazar | SUPERVISOR | {motivo: string} |
| POST /solicitudes-compra/{id}/confirmar-duplicidad | SUPERVISOR | {originalId: number, motivo: string} |
| POST /solicitudes-compra/{id}/emitir-orden | COMPRAS | sin cuerpo |
| GET /ordenes-compra y /ordenes-compra/{id} | ALMACEN, COMPRAS, SUPERVISOR, ADMINISTRADOR | filtros o id |
| POST /ordenes-compra/{id}/recepciones | ALMACEN | {cantidad: number, motivo: string, claveIdempotencia: string} |
| GET /ordenes-compra/{id}/recepciones | ALMACEN, COMPRAS, SUPERVISOR, ADMINISTRADOR | pagina, tamanio |
| POST /ordenes-compra/{id}/cancelar | SUPERVISOR | {motivo: string} |
| GET /conteos | ALMACEN, SUPERVISOR | sku?, almacenId?, confirmado?, desde?, hasta?, pagina, tamanio |
| GET /dashboard | SUPERVISOR, ADMINISTRADOR | desde, hasta, sku?, almacenId? |
| GET /indicadores | SUPERVISOR, ADMINISTRADOR | codigo I05-I10, desde, hasta, sku?, almacenId? |
| GET /evidencias | ADMINISTRADOR | tipo: todos, requisitos, seguridad, e2e o reglas |
| GET /salud | SUPERVISOR, ADMINISTRADOR | sin cuerpo |

Los contratos de catalogo, politicas, movimientos, transferencias, conteos y cortes de APF2
se conservan. CONSULTA_COMERCIAL mantiene consulta de catalogo/saldos/movimientos/reportes,
sin acceso al proceso interno de compras ni indicadores.

CrearSolicitud:

```json
{
  "productoId": 1,
  "almacenId": 1,
  "cantidad": 68,
  "motivo": "Reposicion segun consumo",
  "montoEstimado": 3400.00,
  "claveIdempotencia": "UUID-generado-por-el-cliente",
  "urgente": true,
  "motivoUrgencia": "Cobertura menor al plazo de suministro"
}
```

No generar otra clave al reintentar por timeout. Una nueva necesidad si requiere otra clave.
Un reintento de creacion devuelve la respuesta original PENDIENTE; consultar GET para obtener
el estado actual. Recepciones reutilizan su clave ante reintentos.
POST de compras responde 200 tanto al crear como al devolver un resultado idempotente.
Estas solicitudes se limitan a una linea (producto/almacen); no hay cabecera con varias lineas.

Filtros de listas de solicitudes/ordenes: sku, almacenId, estado, desde, hasta, pagina, tamanio.
Estados de solicitud PENDIENTE/APROBADA/RECHAZADA; orden EMITIDA/ATENDIDA/CANCELADA.
Respuesta Page: content, totalElements, totalPages, number y size. Pagina base 0, tamanio 1..100.
Solicitudes incluyen similaresAbiertas, fechas, monto, actores y auditoria de duplicidad.
Ordenes incluyen cantidad, cantidadRecibida y cantidadPendiente; una cancelada conserva lo recibido.
Recepcion incluye movimientoId para navegar al kardex.

## Indicadores, fechas y errores

GET /dashboard devuelve seis IndicadorResponse. Cada uno contiene codigo, nombre, unidad,
numerador, denominador, valor, sinDatos, desde, hasta y lote cuando utiliza un corte.
I08 usa dias y un numerador en segundos; los otros cinco usan porcentaje.
Mostrar "Sin datos" cuando valor es null. No convertirlo a 0%.
El dashboard requiere politica para cada par critico seleccionado en I10. El corte se genera
explicitamente por SUPERVISOR con POST /cortes-stock; el GET no altera inventario.
Enviar fechas ISO-8601 con zona, por ejemplo 2026-11-09T05:00:00Z para medianoche de Lima.
Hasta es exclusivo. No enviar fechas locales sin zona.

400: campos/periodo invalidos; 401: token ausente, invalido o expirado;
403: rol indebido o cambio de password obligatorio (codigo CAMBIO_PASSWORD_REQUERIDO); 404: recurso inexistente; 409: estado incompatible, saldo insuficiente,
conteo obsoleto o clave idempotente con otro contenido.
Errores de negocio usan ProblemDetail: status, title, detail y errores[] de campo cuando aplica.
Los rechazos de la cadena de seguridad pueden no incluir ProblemDetail; Angular debe usar el status.

CORS permite http://localhost:4200 por defecto; CORS_ORIGINS configura una lista separada por comas.
Permite Authorization y Content-Type. No se habilitan cookies ni origen comodin con credenciales.
Las claves de PostgreSQL, JWT_SECRET y claves de InsForge pertenecen exclusivamente al servidor.
