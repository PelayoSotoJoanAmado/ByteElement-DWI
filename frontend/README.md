# StockGuard · Frontend

Angular 22, componentes standalone, Router y SCSS. Proyecto generado con Angular CLI; las dependencias quedan fijadas en `package-lock.json`.

## Arranque

Usar una versión de Node admitida por `package.json` (desarrollo verificado con Node 24.22.1).

```bash
cd frontend
npm ci
npm start
```

Abrir http://localhost:4200. Esta dirección coincide con el origen permitido por el backend.

```bash
npm run build
npm test -- --watch=false
```

La compilación de producción se genera en `dist/frontend/browser/`. El servidor de despliegue debe devolver `index.html` para las rutas de la SPA, como `/login`.

## Alcance de esta entrega

- `/`: presentación pública responsive, fondo original optimizado en WebP, funcionalidades, funcionamiento y footer completo.
- Menú público sin opción «Inicio»; el logo vuelve a la presentación.
- Navegación a secciones, menú móvil, enlace para saltar al contenido, foco visible y preferencias de movimiento reducido.
- `/login`: interfaz de acceso con tarjeta redondeada a la derecha, fondo de almacén, campos usuario/contraseña, validaciones, mostrar/ocultar contraseña y estados de carga/error.
- Se eliminó el bloque de ayuda de cuentas y el texto de roles del formulario. No hay registro público.
- El formulario envía `POST /api/v1/auth/login`. Al recibir una respuesta válida, conserva token, usuario y vencimiento en `sessionStorage` (memoria si el almacenamiento está bloqueado), limpia la contraseña y redirige a `/admin` si el rol es `ADMINISTRADOR`. Los otros roles conservan la presentación pública como destino hasta desarrollar sus áreas de trabajo.
- `/admin`: panel de administrador con menú lateral, resumen sin datos, accesos rápidos, ilustración original y cierre de sesión. Adaptación a móvil con menú desplegable.
- Las rutas públicas y el panel se cargan por separado mediante lazy loading. Las URL desconocidas vuelven a la presentación.
- Los enlaces internos del panel apuntan a rutas protegidas con un aviso «Próximamente». Usuarios, alta de cuentas, productos, almacenes y consultas aún no realizan operaciones ni muestran datos del backend.
- No se muestran datos reales del inventario en esta entrega. La interfaz de registro será un apartado exclusivo del administrador.

## Conexión al backend y próximos pasos

El contrato de login sigue `backend/docs/APF3/API-ANGULAR.md`. En desarrollo, `proxy.conf.json` reenvía `/api/**` a `http://localhost:8080`; arrancar el backend por separado. Ajustar el destino si el backend usa otro puerto. En producción, configurar el mismo origen con un proxy inverso para `/api/`. No se incluyen secretos ni credenciales.
Respetar los cinco roles exactos: `ALMACEN`, `COMPRAS`, `SUPERVISOR`, `CONSULTA_COMERCIAL` y `ADMINISTRADOR`.
El panel y sus rutas hijas requieren sesión vigente con rol `ADMINISTRADOR`. Los accesos sin sesión se redirigen al login; los otros cuatro roles reciben un aviso de falta de permiso. Una sesión de administrador existente redirige desde el login al panel.

El interceptor envía Bearer únicamente a rutas relativas `/api/v1/`, excluyendo login, y cierra la sesión ante 401 de la misma sesión. Un 403 no la elimina y un 401 tardío de una sesión anterior no cierra una sesión nueva. El panel vigila el vencimiento mientras permanece abierto; cerrar sesión elimina el token y vuelve al login. La sesión local también comprueba el vencimiento al consultarla y nunca incluye la contraseña. Ocultar un enlace no sustituye la autorización; el backend conserva la validación final.
El formulario de login no permite elegir rol. El menú del administrador omite operaciones de almacén y aprobaciones asignadas a otros roles; sus enlaces de compras y movimientos son consultas. La comprobación de sesión en el cliente no sustituye la validación del JWT en el servidor.

Verificación: compilación de producción y 28 pruebas de login, navegación, redirección de administrador, rechazo de los otros cuatro roles, acceso por URL directa y rutas hijas, logout, expiración y alcance del interceptor Bearer. Las respuestas de autenticación se verifican con HttpTestingController; queda pendiente la prueba integrada con backend y cuenta reales.

## Organización

- `src/app/pages/home/`: pantalla pública.
- `src/app/pages/login/`: página de acceso y componente de formulario.
- `src/app/core/auth/`: modelos de los cinco roles, cliente de login, sesión, guards e interceptor.
- `src/app/pages/admin/`: estructura del panel, menú, vista general y apartados pendientes.
- `public/images/admin-warehouse.webp`: ilustración decorativa del panel con transparencia.
- `src/app/shared/icon.ts`: iconos SVG y marca.
- `src/styles.scss` y `src/styles/_home.scss`: estilos globales, página y adaptaciones responsive.
- `public/images/warehouse-hero.webp`: fondo generado específicamente para StockGuard.

## Imagen de fondo

Generada con ImageGen a partir de un almacén moderno con estanterías, cajas y transpaleta, iluminación turquesa, sombras azul oscuro y espacio despejado a la izquierda. Se exportó a WebP sin cambiar la composición. Los textos, tarjetas, enlaces y botones son HTML real; no forman parte de la imagen.

## Revisión visual y prueba integrada

La revisión visual del panel se hizo con HTML renderizado por los componentes Angular en un fixture de pruebas con una sesión ficticia. Esa sesión solo se usó en el entorno de pruebas; no hay ruta pública de demostración, token precargado ni excepción a los guards en la aplicación. La ilustración se generó con ImageGen y se optimizó en WebP con canal alfa.

La siguiente etapa es arrancar backend y base de datos reales y verificar login → panel → cierre de sesión, además de conectar los módulos que ahora muestran «Próximamente». No se modificó el backend ni se crearon usuarios o registros reales en esta entrega.
