# Validación local del administrador

Existe una [revision integrada ejecutada](../../backend/docs/APF3/revision-integrada/README.md).
La lista de abajo sirve para repetir comprobaciones manuales sobre una base de pruebas.

Requisitos: backend y PostgreSQL encendidos, frontend actualizado en la rama `front`, cuenta de administrador válida. Las pruebas siguientes crean datos de prueba en la base que hayas configurado; usa tu entorno local de pruebas.

1. Inicia Angular con `npm start`, entra por `/login` y comprueba la redirección a `/admin`.
2. Abre **Estado del sistema**. La respuesta debe indicar servicio disponible y base conectada. Si falla la petición, debe mostrarse un error, nunca un estado saludable simulado.
3. En **Almacenes**, crea código `UI-PRUEBA` y nombre `Almacén de prueba de interfaz`. Comprueba que aparece en el listado y el detalle. Edita el nombre y verifica el cambio tras actualizar la página.
4. En **Productos**, crea SKU `UI-PRUEBA-001`, categoría `Pruebas`, marca `Demo`, modelo `Interfaz`, especificación `Registro de prueba del frontend`, crítico marcado. Comprueba listado/detalle y edición de la especificación. Repetir el SKU debe mostrar el conflicto del servidor.
5. En **Existencias**, selecciona ese producto y almacén. Sin movimientos previos, la cantidad debe ser 0. La falta de conexión no debe mostrarse como stock 0.
6. En Usuarios, crea una cuenta de prueba con rol COMPRAS y una clave temporal privada de minimo
12 caracteres y maximo 72 bytes UTF-8. En Detalle deben aparecer edicion, estado, reset e historial
para cuentas ajenas. La propia cuenta no se administra desde esa tabla. Al entrar con la cuenta
nueva, completar el cambio obligatorio antes de acceder a Compras. Consultar el contrato V8.
7. Vuelve al **Panel general**. Los contadores deben reflejar usuarios, productos y almacenes activos; una consulta fallida debe mostrar «No disponible». Las alertas dependen de las políticas configuradas por el supervisor.
8. En **Movimientos**, prueba filtros de SKU, almacén y fechas. No debe haber acciones de registro. Un listado vacío es válido si no hay operaciones en el periodo.
9. En **Compras**, recorre Reposición, Solicitudes y Órdenes. Comprueba filtros, páginas y detalles. Si existe una orden, revisa sus recepciones. No deben aparecer aprobación, emisión ni recepción para el administrador.
10. En **Reportes**, consulta rotación y stock crítico. En **Indicadores**, consulta I05–I10. La fecha final incluye el día completo en horario de Lima. «Sin datos» es un resultado válido y no debe convertirse en 0 %. Algunos resultados requieren cortes/políticas de supervisor y operaciones de otros roles.
11. En **Evidencias**, revisa categorías, fecha y alcance. El archivo del repositorio contiene evidencia histórica de backend; la consulta no ejecuta pruebas nuevas.
12. Desactiva solo el producto/almacén de prueba y confirma el aviso. Verifica el estado inactivo y que no se haya borrado el historial.
13. Cierra sesión e intenta abrir `/admin/usuarios` directamente: debe volver al login. Inicia con la cuenta COMPRAS y prueba `/admin`: debe rechazar el acceso al panel del administrador.

Si aparece un error, registra la pantalla, el estado HTTP y el mensaje de respuesta desde la pestaña Red del navegador. No compartas contraseñas, cabeceras Authorization ni tokens.
