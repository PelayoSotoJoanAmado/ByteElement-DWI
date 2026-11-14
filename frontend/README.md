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
- El formulario envía `POST /api/v1/auth/login`. Al recibir una respuesta válida, conserva token, usuario y vencimiento en `sessionStorage` (memoria si el almacenamiento está bloqueado), limpia la contraseña y vuelve temporalmente a la presentación. El destino interno se incorporará al desarrollar el área de trabajo.
- Las rutas públicas se cargan por separado mediante lazy loading. Las URL desconocidas vuelven a la presentación.
- No se muestran datos reales del inventario en esta entrega. La interfaz de registro será un apartado exclusivo del administrador.

## Conexión al backend y próximos pasos

El contrato de login sigue `backend/docs/APF3/API-ANGULAR.md`. En desarrollo, `proxy.conf.json` reenvía `/api/**` a `http://localhost:8080`; arrancar el backend por separado. Ajustar el destino si el backend usa otro puerto. En producción, configurar el mismo origen con un proxy inverso para `/api/`. No se incluyen secretos ni credenciales.
Respetar los cinco roles exactos: `ALMACEN`, `COMPRAS`, `SUPERVISOR`, `CONSULTA_COMERCIAL` y `ADMINISTRADOR`.
Al añadir módulos internos, completar el interceptor Bearer, manejo global de expiración/401, guards y permisos por ruta conforme a la matriz del backend. La sesión local comprueba el vencimiento al consultarla y nunca incluye la contraseña. Ocultar un enlace no sustituye la autorización; el backend conserva la validación final.
Esta entrega no define rutas internas ni otorga acceso por un rol elegido en el formulario. La comprobación de sesión en el cliente no sustituye la validación del JWT en el servidor.

Verificación: compilación de producción, pruebas de navegación, validación de campos, visibilidad de contraseña, envío sin alterar la contraseña, prevención de doble envío, respuestas 401/503 y control de sesiones vencidas. Las respuestas de autenticación se verifican con HttpTestingController; queda pendiente la prueba integrada con backend y cuenta reales.

## Organización

- `src/app/pages/home/`: pantalla pública.
- `src/app/pages/login/`: página de acceso y componente de formulario.
- `src/app/core/auth/`: modelos de los cinco roles, cliente de login y sesión.
- `src/app/shared/icon.ts`: iconos SVG y marca.
- `src/styles.scss` y `src/styles/_home.scss`: estilos globales, página y adaptaciones responsive.
- `public/images/warehouse-hero.webp`: fondo generado específicamente para StockGuard.

## Imagen de fondo

Generada con ImageGen a partir de un almacén moderno con estanterías, cajas y transpaleta, iluminación turquesa, sombras azul oscuro y espacio despejado a la izquierda. Se exportó a WebP sin cambiar la composición. Los textos, tarjetas, enlaces y botones son HTML real; no forman parte de la imagen.
