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
- `/login`: destino temporal explícito «Próximamente», con retorno a la presentación. No autentica ni solicita credenciales.
- Las rutas públicas se cargan por separado mediante lazy loading. Las URL desconocidas vuelven a la presentación.
- No se consulta la API ni se muestran datos reales del inventario en esta entrega.

## Siguiente entrega: autenticación y permisos

Integrar `POST /api/v1/auth/login` conforme a `backend/docs/APF3/API-ANGULAR.md`.
Respetar los cinco roles exactos: `ALMACEN`, `COMPRAS`, `SUPERVISOR`, `CONSULTA_COMERCIAL` y `ADMINISTRADOR`.
Al añadir módulos internos, implementar sesión, interceptor Bearer, expiración/401, guards y permisos por ruta conforme a la matriz del backend. Ocultar un enlace no sustituye la autorización; el backend conserva la validación final.
Esta entrega no define rutas internas ni simula una sesión o permisos.

## Organización

- `src/app/pages/home/`: pantalla pública.
- `src/app/pages/login-pending/`: destino temporal del acceso.
- `src/app/shared/icon.ts`: iconos SVG y marca.
- `src/styles.scss` y `src/styles/_home.scss`: estilos globales, página y adaptaciones responsive.
- `public/images/warehouse-hero.webp`: fondo generado específicamente para StockGuard.

## Imagen de fondo

Generada con ImageGen a partir de un almacén moderno con estanterías, cajas y transpaleta, iluminación turquesa, sombras azul oscuro y espacio despejado a la izquierda. Se exportó a WebP sin cambiar la composición. Los textos, tarjetas, enlaces y botones son HTML real; no forman parte de la imagen.
