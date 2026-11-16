# Cambios necesarios en el informe

Para actualizar Consistencia, Operacionalización, Trazabilidad y la comparación de resultados,
seguir [GUIA-INDICADORES-Y-TRAZABILIDAD.md](../../../GUIA-INDICADORES-Y-TRAZABILIDAD.md).

Complementar esta lista con [cuentas/CAMBIOS-INFORME.md](cuentas/CAMBIOS-INFORME.md),
actualizada el 16/11: RF09, primer acceso, cuentas, V8, 49 contratos y cambios de diagramas/modelo.
Los resultados de volumen anteriores conservan su fecha y alcance.

Esta lista corresponde al backend implementado para APF3. El equipo debe trasladar estos cambios
al Word/PDF, actualizar capturas con su propia ejecucion y completar la evidencia Angular.
No se modifico el PDF original ni se atribuyen resultados reales a ByteElement.

## Capitulo I

1. Conservar linea base como simulada: 15%, 23%, 89%, 2.7 dias, 8%, 11%.
2. Agregar el anexo E mencionado pero ausente: copiar y adaptar REGLAS-Y-MODELO.md.
3. Matriz de operacionalizacion: I08 filtra por fecha de aprobacion; I09 por fecha de envio y
   solo duplicidades confirmadas. I07 usa observaciones producto/almacen/fecha, no el saldo ya ajustado.
4. Mantener I05 como evento durante la semana e I10 como estado al cierre; no intercambiarlos.
5. Explicitar periodo sin datos, numerador, denominador, redondeo y unidad dias de I08.
6. Actualizar trazabilidad: endpoints reales de API-ANGULAR.md, campos de V6/V7, pruebas y evidencia.
   I01-I04 del backend no equivalen automaticamente a aceptacion del frontend o del despliegue.
7. No concluir mejora causal ni usar los porcentajes de la carga como resultados reales.
   La comparacion antes/despues requiere escenarios equivalentes de demanda, catalogo y periodo.

## Capitulo II

1. RF03 debe describir consumo de SALIDA, pedidos pendientes, punto de reposicion y cantidad sugerida.
2. RF04 debe distinguir advertencia, confirmacion de duplicidad y reintento idempotente.
3. RF05 debe incluir monto previo a aprobar, umbral y justificacion; decision unica; orden unica;
   urgencia con motivo; recepcion parcial y cancelacion de pendiente.
4. RF07 debe describir saldo/ultimo movimiento al corte, autorizacion y rechazo por obsolescencia.
5. RF08 debe documentar los seis indicadores verificables; agregar las pruebas conocidas.
6. RF10 debe mostrar reporte por version, ocho reglas y cinco flujos, distinguiendo backend
   de E2E Angular. No marcar aceptado todo el proyecto antes de integrar y desplegar.
7. Unificar permisos: COMPRAS solicita y emite; SUPERVISOR decide; ALMACEN recibe y mueve.
   Corregir el handover anterior, que permitia solicitar a Almacen y emitir al Supervisor.
   La matriz I02 actual contiene 49 contratos y comprueba todos los roles. Separar autorizacion
   permitida (puede terminar en validacion 400/404) de una operacion de negocio aprobada.
8. MVC: Angular es la vista. Las plantillas "Hola mundo" de resources/templates son vestigios,
   no evidencia de una interfaz terminada ni de vistas renderizadas por Spring.
9. Conexion: mantener PostgreSQL/JPA/Hibernate/Flyway; V1-V8. InsForge es alojamiento opcional
   de PostgreSQL mediante JDBC, no reemplazo de Spring Boot ni del ORM.
10. ER/logico/fisico: 12 tablas de negocio mas control_cuentas tecnica (13 entidades JPA);
    agregar evento_usuario y control_cuentas segun la guia de cuentas. En el corte anterior habia 11 tablas de negocio; agregar recepcion_compra y sus FK a orden, usuario
    y movimiento; cantidad_recibida/cancelacion; monto, urgencia, fechas de revision,
    confirmacion de duplicidad y respuesta idempotente en solicitudes.
    flyway_schema_history es tecnica; no contarla como entidad del negocio.
11. Ampliar diagrama por capas con ComprasService, ReposicionService, controladores, DTOs y repositorios.
12. TDD: conservar evidencia historica genuina y agregar pruebas nuevas. No inventar capturas
    Red previas: la existencia de pruebas finales no demuestra por si sola el orden TDD de desarrollo.

## Capitulo III

1. Presentar flujos F01 movimiento, F02 transferencia, F03 reposicion/compra/recepcion,
   F04 conteo/ajuste y F05 dashboard/permisos desde Angular.
2. Mostrar respuestas HTTP, datos antes/despues y persistencia; capturas con actor y periodo.
3. Reportar version/huella y resultado de la suite desde evidencias/resultado.json.
4. Carga: 10000 productos, 30000 existencias, 30000 politicas, 60000 movimientos,
   1000 conteos, 2000 solicitudes, 1840 ordenes y 30000 filas de corte.
   Son 164840 filas de negocio nuevas; los usuarios y almacenes son adicionales.
5. En una base exclusiva con solo este catalogo, para los ultimos siete dias:
   I05=10%, I06=20%, I07=90%, I08=2 dias, I09=8%, I10=10%.
   Son valores de control sinteticos; no son una mejora probada.
6. Mostrar medicion del entorno, latencia promedio/p95 y errores. Declarar concurrencia y numero
   de peticiones; no prometer un SLA a partir de una prueba secuencial.
7. Mostrar logs de metodo/ruta/status/duracion sin passwords ni JWT.
8. RNF05: documentar backup y restauracion real en una base distinta, contrastando saldos,
   solicitudes, ordenes, recepciones e historial. No basta una captura de archivo de backup.
9. Agregar pendientes reales: interfaz Angular, E2E en navegador y despliegue HTTPS.
