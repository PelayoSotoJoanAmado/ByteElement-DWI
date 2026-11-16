const fs = require('fs');
const crypto = require('crypto');
const assert = require('assert/strict');
const { chromium } = require(process.env.STOCKGUARD_E2E_PLAYWRIGHT || 'playwright');
const configPath = process.env.STOCKGUARD_E2E_CONFIG;
assert(configPath, 'Define STOCKGUARD_E2E_CONFIG con un archivo privado de una base de pruebas');
const cfg = JSON.parse(fs.readFileSync(configPath, 'utf8'));
assert(
  cfg.confirmarBaseAislada === true && /test|prueba/i.test(cfg.baseDatos || ''),
  'Confirmar una base aislada de pruebas',
);
assert(
  cfg.adminExistente?.password && !cfg.adminExistente.password.startsWith('REEMPLAZAR'),
  'Configura la clave privada del administrador de pruebas',
);
assert(/^https?:\/\//.test(cfg.base), 'Configura la URL de Angular con su origen CORS autorizado');
const roles = {
  admin: 'ADMINISTRADOR',
  almacen: 'ALMACEN',
  compras: 'COMPRAS',
  supervisor: 'SUPERVISOR',
  consulta: 'CONSULTA_COMERCIAL',
};
const randomPassword = () => crypto.randomBytes(24).toString('base64url');
cfg.cuentas ??= Object.fromEntries(
  Object.entries(roles).map(([key, rol]) => [
    key,
    { username: 'revision-' + key, rol, inicial: randomPassword(), nueva: randomPassword() },
  ]),
);
cfg.temporalReset ??= randomPassword();
cfg.finalReset ??= randomPassword();
const run = Date.now().toString(36) + '-' + crypto.randomBytes(3).toString('hex');
for (const c of Object.values(cfg.cuentas)) c.username += '-' + run;
const report = {
  fecha: new Date().toISOString(),
  base: cfg.baseDatos,
  navegador: 'Chromium',
  simulacionesHttp: false,
  casos: [],
  erroresNavegador: [],
  capturas: [],
};
const out = 'backend/target/revision-e2e-resultado.json';
const pages = {};
const lastWrites = {};
const secrets = [
  cfg.adminExistente.password,
  cfg.temporalReset,
  cfg.finalReset,
  ...Object.values(cfg.cuentas).flatMap((c) => [c.inicial, c.nueva]),
];
let browser;
function scrub(text) {
  for (const s of secrets) text = text.split(s).join('[OMITIDO]');
  return text.replace(/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/g, '[JWT OMITIDO]');
}
function save() {
  fs.writeFileSync(out, JSON.stringify(report, null, 2));
}
async function step(name, fn) {
  try {
    const detail = await fn();
    report.casos.push({ caso: name, aprobado: true, ...detail });
    console.log('OK ' + name);
    save();
  } catch (e) {
    report.casos.push({ caso: name, aprobado: false, error: scrub(e.message) });
    for (const [key, p] of Object.entries(pages)) {
      await p.screenshot({ path: 'backend/target/revision-fallo-' + key + '.png' }).catch(() => {});
      fs.writeFileSync('backend/target/revision-fallo-' + key + '.html', scrub(await p.content()));
    }
    save();
    throw e;
  }
}
async function pageFor(key) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
  await context.addInitScript(() => {
    window.__realResponses = [];
    const nativeFetch = window.fetch.bind(window);
    window.fetch = (...args) =>
      nativeFetch(...args).then((response) => {
        const method = (args[0] instanceof Request ? args[0].method : args[1]?.method) || 'GET';
        if (new URL(response.url).pathname.startsWith('/api/v1/'))
          response
            .clone()
            .json()
            .then((body) =>
              window.__realResponses.push({
                method,
                path: new URL(response.url).pathname,
                status: response.status,
                body,
              }),
            )
            .catch(() => {});
        return response;
      });
  });
  const page = await context.newPage();
  page.setDefaultTimeout(12000);
  page.on('dialog', (d) => d.accept());
  page.on('pageerror', (e) => report.erroresNavegador.push(scrub(e.message)));
  page.on('framenavigated', (f) => {
    if (f === page.mainFrame()) console.log('NAV ' + key + ' ' + new URL(f.url()).pathname);
  });
  pages[key] = page;
  return page;
}
const fc = (p, name) =>
  p.locator(
    `[formcontrolname="${name}"], #field-${name}${['critico', 'activo'].includes(name) ? ', input[type=checkbox]' : ''}`,
  );
const openDialog = (p) => p.locator('dialog[open]').last();
async function observed(p, method, path, status) {
  await p.waitForFunction(
    ({ method, path, status }) =>
      window.__realResponses?.some(
        (r) => r.method === method && r.path === '/api/v1/' + path && r.status === status,
      ),
    { method, path, status },
  );
  return p.evaluate(
    ({ method, path, status }) => {
      const i = window.__realResponses.findIndex(
        (r) => r.method === method && r.path === '/api/v1/' + path && r.status === status,
      );
      return window.__realResponses.splice(i, 1)[0].body;
    },
    { method, path, status },
  );
}
async function uiRequest(p, method, path, action, expected = 200) {
  const body = p
    .waitForResponse(
      (r) => r.request().method() === method && new URL(r.url()).pathname === '/api/v1/' + path,
    )
    .then(async (response) => {
      console.log('RESP ' + method + ' ' + path + ' ' + response.status());
      assert.equal(response.status(), expected, method + ' ' + path);
      if (method === 'POST' && path.endsWith('/recepciones'))
        lastWrites[path] = response.request().postDataJSON();
      try {
        return await response.json();
      } catch (e) {
        if (expected >= 400) return null;
        return observed(p, method, path, expected);
      }
    });
  const [data] = await Promise.all([body, action()]);
  return data;
}
async function session(p) {
  const s = await p.evaluate(() => JSON.parse(sessionStorage.getItem('stockguard.session')));
  assert(s);
  secrets.push(s.accessToken);
  return s;
}
async function api(p, method, path, data, status = 200, token) {
  token ??= (await session(p)).accessToken;
  const r = await p.request.fetch(cfg.base + '/api/v1/' + path, {
    method,
    headers: { Authorization: 'Bearer ' + token },
    data,
  });
  assert.equal(r.status(), status, method + ' ' + path);
  return r.status() === 204 ? null : r.json().catch(() => null);
}
async function login(p, username, password, pending = false) {
  await p.goto(cfg.base + '/login');
  await fc(p, 'username').fill(username);
  await fc(p, 'password').fill(password);
  const data = await uiRequest(p, 'POST', 'auth/login', () =>
    p.getByRole('button', { name: 'Iniciar sesión', exact: true }).click(),
  );
  secrets.push(data.accessToken);
  await p.waitForURL(
    pending ? '**/cuenta/cambiar-password' : (url) => !url.pathname.startsWith('/login'),
  );
  return data;
}
async function change(p, current, next) {
  await fc(p, 'passwordActual').fill(current);
  await fc(p, 'passwordNueva').fill(next);
  await fc(p, 'confirmacionPassword').fill(next);
  const data = await uiRequest(p, 'POST', 'auth/cambiar-password', () =>
    p.getByRole('button', { name: 'Cambiar contraseña', exact: true }).click(),
  );
  secrets.push(data.accessToken);
  await p.waitForURL((url) => !url.pathname.includes('cambiar-password'));
  return data;
}
async function shot(p, name) {
  const dest = 'backend/target/revision-' + name + '.png';
  await p.screenshot({ path: dest, fullPage: true });
  report.capturas.push(dest);
}
async function userDetail(p, name) {
  await p.goto(cfg.base + '/admin/usuarios');
  await p.locator('input[type=search]').fill(name);
  await p.getByRole('button', { name: 'Ver detalle de ' + name, exact: true }).click();
  return openDialog(p);
}
async function editUser(p, name, apply) {
  let d = await userDetail(p, name);
  await d.getByRole('button', { name: 'Editar cuenta, rol y estado', exact: true }).click();
  d = openDialog(p);
  await apply(d);
  return d;
}
async function userSave(p, expected = 200) {
  const d = openDialog(p);
  const form = await d.locator('form').count();
  assert(form);
  const id = await fc(d, 'username').inputValue();
  const [r] = await Promise.all([
    p.waitForResponse(
      (r) =>
        r.request().method() === 'PUT' &&
        /\/api\/v1\/usuarios\/\d+$/.test(new URL(r.url()).pathname),
    ),
    d.getByRole('button', { name: 'Guardar usuario', exact: true }).click(),
  ]);
  assert.equal(r.status(), expected, 'Editar ' + id);
  return r.json().catch(() => null);
}
async function stock(p, product, warehouse) {
  return (await api(p, 'GET', `existencias?productoId=${product}&almacenId=${warehouse}`)).cantidad;
}
async function warehouseForm(p, path, values, endpoint, status = 201) {
  await p.goto(cfg.base + '/almacen/' + path);
  const f = p.locator('form.warehouse-form');
  for (const [key, value] of Object.entries(values)) {
    const c = fc(f, key);
    if (await c.evaluate((e) => e.tagName === 'SELECT')) await c.selectOption(String(value));
    else await c.fill(String(value));
  }
  if (path === 'recepciones')
    await f.getByRole('button', { name: 'Consultar orden', exact: true }).click();
  if (status === 0) {
    let writes = 0;
    const listener = (r) => {
      if (r.method() === 'POST' && new URL(r.url()).pathname === '/api/v1/' + endpoint) writes++;
    };
    p.on('request', listener);
    await f.getByRole('button', { name: /^Registrar / }).click();
    await p.getByRole('alert').last().waitFor();
    p.off('request', listener);
    assert.equal(writes, 0, 'La validacion local no debe enviar la recepcion excesiva');
    return null;
  }
  const response = await uiRequest(
    p,
    'POST',
    endpoint,
    () => f.getByRole('button', { name: /^Registrar / }).click(),
    status,
  );
  return response;
}
async function purchase(p, product, warehouse, motive) {
  await p.goto(cfg.base + '/compras/solicitudes');
  await p.getByRole('button', { name: 'Nueva solicitud', exact: true }).click();
  const f = p.locator('form').filter({ has: fc(p, 'montoEstimado') });
  await fc(f, 'productoId').selectOption(String(product));
  await fc(f, 'almacenId').selectOption(String(warehouse));
  await fc(f, 'cantidad').fill('10');
  await fc(f, 'montoEstimado').fill('6000');
  await fc(f, 'motivo').fill(motive);
  await fc(f, 'urgente').check();
  await fc(f, 'motivoUrgencia').fill('Cobertura de prueba menor al plazo de suministro');
  const row = await uiRequest(p, 'POST', 'solicitudes-compra', () =>
    f.getByRole('button', { name: 'Enviar solicitud', exact: true }).click(),
  );
  await openDialog(p).waitFor();
  return row;
}
async function purchaseDetail(p, role, tab, sku, id) {
  await p.goto(cfg.base + '/' + role + '/' + tab);
  await fc(p, 'sku').fill(sku);
  await p
    .locator('form.ap-toolbar')
    .getByRole('button', { name: 'Consultar', exact: true })
    .click();
  const row = p
    .locator('tbody tr')
    .filter({
      has: p.locator('td:first-child').filter({ hasText: new RegExp('^\\s*' + id + '\\s*$') }),
    });
  await row.getByRole('button', { name: /^Ver detalle de / }).click();
  return openDialog(p);
}

(async () => {
  browser = await chromium.launch({
    headless: true,
    ...(process.env.STOCKGUARD_E2E_CHROMIUM
      ? { executablePath: process.env.STOCKGUARD_E2E_CHROMIUM }
      : {}),
  });
  report.navegador = browser.version();
  let admin = await pageFor('original');
  await step('login-administrador-existente-y-creacion-cinco-roles', async () => {
    await login(admin, cfg.adminExistente.username, cfg.adminExistente.password);
    const users = await api(admin, 'GET', 'usuarios');
    assert(
      users.some(
        (u) => u.username === cfg.adminExistente.username && u.rol === 'ADMINISTRADOR' && u.activo,
      ),
      'Administrador de pruebas activo requerido',
    );
    for (const c of Object.values(cfg.cuentas)) {
      await admin.goto(cfg.base + '/admin/usuarios');
      await admin.getByRole('button', { name: /\+ Crear usuario/ }).click();
      const d = openDialog(admin);
      await fc(d, 'username').fill(c.username);
      await fc(d, 'password').fill(c.inicial);
      await fc(d, 'confirm').fill(c.inicial);
      await fc(d, 'rol').selectOption(c.rol);
      const u = await uiRequest(
        admin,
        'POST',
        'usuarios',
        () => d.getByRole('button', { name: 'Guardar usuario', exact: true }).click(),
        201,
      );
      assert(u.requiereCambioPassword);
      c.id = u.id;
    }
    return { cuentasCreadas: 5, rolesDistintos: 5 };
  });
  const homes = {
    admin: '/admin',
    almacen: '/almacen',
    compras: '/compras/solicitudes',
    supervisor: '/supervisor/indicadores',
    consulta: '/consulta/existencias',
  };
  for (const [key, c] of Object.entries(cfg.cuentas))
    await step('primer-acceso-y-permisos-' + c.rol, async () => {
      const p = await pageFor(key),
        traffic = [];
      p.on('request', (r) => {
        if (new URL(r.url()).pathname.startsWith('/api/v1/'))
          traffic.push(new URL(r.url()).pathname);
      });
      const before = await login(p, c.username, c.inicial, true);
      await p.goto(cfg.base + homes[key]);
      await p.waitForURL('**/cuenta/cambiar-password');
      assert(
        traffic.every((path) => path.startsWith('/api/v1/auth/')),
        'Se consultaron modulos antes del cambio',
      );
      if (key === 'almacen') {
        await fc(p, 'passwordActual').fill('ClaveIncorrecta123!');
        await fc(p, 'passwordNueva').fill(c.nueva);
        await fc(p, 'confirmacionPassword').fill(c.nueva);
        await uiRequest(
          p,
          'POST',
          'auth/cambiar-password',
          () => p.getByRole('button', { name: 'Cambiar contraseña', exact: true }).click(),
          400,
        );
        assert(await session(p));
      }
      const changed = await change(p, c.inicial, c.nueva),
        now = await session(p);
      assert.equal(now.accessToken, changed.accessToken);
      assert(!now.usuario.requiereCambioPassword);
      await api(p, 'GET', 'auth/me', undefined, 401, before.accessToken);
      assert(new URL(p.url()).pathname.startsWith(homes[key].split('/').slice(0, 2).join('/')));
      if (key !== 'admin') {
        await p.goto(cfg.base + '/admin/usuarios');
        await p.waitForURL('**/acceso-denegado');
        assert(await session(p));
        await p.goto(cfg.base + homes[key]);
      }
      return {
        sinPeticionesOperativasAntesDelCambio: true,
        tokenAnterior: 401,
        cambio: 200,
        guardPorRol: true,
      };
    });
  let product, wh1, wh2;
  const sku = ('REV-FLUJO-' + run).toUpperCase();
  await step('catalogo-y-politicas-minimo-maximo', async () => {
    await admin.goto(cfg.base + '/admin/productos');
    await admin.getByRole('button', { name: /\+ Nuevo producto/ }).click();
    const d = openDialog(admin);
    for (const [k, v] of Object.entries({
      sku,
      categoria: 'Prueba integrada',
      marca: 'Revision',
      modelo: 'Flujo real',
      especificacion: 'Dato sintetico en copia aislada',
    }))
      await fc(d, k).fill(v);
    await fc(d, 'critico').check();
    product = await uiRequest(
      admin,
      'POST',
      'productos',
      () => d.getByRole('button', { name: 'Guardar producto', exact: true }).click(),
      201,
    );
    const warehouses = await api(admin, 'GET', 'almacenes');
    wh1 = warehouses.find((w) => w.codigo === 'A01' && w.activo).id;
    wh2 = warehouses.find((w) => w.codigo === 'A02' && w.activo).id;
    const p = pages.supervisor;
    await p.goto(cfg.base + '/supervisor/politicas');
    await fc(p, 'productoId').selectOption(String(product.id));
    await fc(p, 'almacenId').selectOption(String(wh1));
    await fc(p, 'minimo').fill('5');
    await fc(p, 'maximo').fill('5');
    await p.getByRole('button', { name: 'Guardar política', exact: true }).click();
    assert(await p.getByRole('alert').count());
    await fc(p, 'maximo').fill('20');
    await uiRequest(p, 'PUT', `productos/${product.id}/almacenes/${wh1}/politica-stock`, () =>
      p.getByRole('button', { name: 'Guardar política', exact: true }).click(),
    );
    return { productoId: product.id, sku, minimo: 5, maximo: 20 };
  });
  await step('F01-entrada-salida-y-rechazo-saldo-insuficiente', async () => {
    const p = pages.almacen;
    const common = { productoId: product.id, almacenId: wh1, motivo: 'Revision E2E F01' };
    const entry = await warehouseForm(
      p,
      'movimientos',
      { ...common, tipo: 'ENTRADA', cantidad: 10 },
      'movimientos',
    );
    assert.equal(entry.autor, cfg.cuentas.almacen.username);
    await warehouseForm(
      p,
      'movimientos',
      { ...common, tipo: 'SALIDA', cantidad: 3 },
      'movimientos',
    );
    assert.equal(await stock(p, product.id, wh1), 7);
    await warehouseForm(
      p,
      'movimientos',
      { ...common, tipo: 'SALIDA', cantidad: 999 },
      'movimientos',
      409,
    );
    assert.equal(await stock(p, product.id, wh1), 7);
    return { entrada: 10, salida: 3, saldo: 7, salidaInsuficiente: 409, sinEscrituraParcial: true };
  });
  await step('F02-transferencia-y-dos-saldos-trazables', async () => {
    const p = pages.almacen;
    const t = await warehouseForm(
      p,
      'transferencias',
      {
        productoId: product.id,
        almacenId: wh1,
        destinoAlmacenId: wh2,
        cantidad: 2,
        motivo: 'Revision E2E F02',
      },
      'transferencias',
    );
    assert.equal(await stock(p, product.id, wh1), 5);
    assert.equal(await stock(p, product.id, wh2), 2);
    const a = await api(p, 'GET', 'movimientos/' + t.movimientoSalidaId),
      b = await api(p, 'GET', 'movimientos/' + t.movimientoEntradaId);
    assert.equal(a.tipo, 'TRANSFERENCIA_SALIDA');
    assert.equal(b.tipo, 'TRANSFERENCIA_ENTRADA');
    assert.equal(a.autor, b.autor);
    assert.equal(a.cantidad, b.cantidad);
    return {
      origen: 5,
      destino: 2,
      movimientoSalida: t.movimientoSalidaId,
      movimientoEntrada: t.movimientoEntradaId,
      autorYCantidadCoinciden: true,
    };
  });
  let request, duplicate, order;
  await step('F03-solicitudes-urgentes-y-advertencia-de-similitud', async () => {
    request = await purchase(pages.compras, product.id, wh1, 'Revision E2E solicitud original');
    duplicate = await purchase(pages.compras, product.id, wh1, 'Revision E2E posible duplicada');
    assert(duplicate.similaresAbiertas.includes(request.id));
    return {
      solicitud: request.id,
      adicional: duplicate.id,
      urgente: request.urgente,
      similitudDetectada: true,
    };
  });
  await step('F03-confirmar-duplicidad-y-validar-umbral-de-aprobacion', async () => {
    const p = pages.supervisor;
    let d = await purchaseDetail(p, 'supervisor', 'solicitudes', sku, duplicate.id);
    await fc(d, 'motivo').fill('Duplicidad confirmada en prueba real');
    await fc(d, 'originalId').fill(String(request.id));
    const dup = await uiRequest(
      p,
      'POST',
      `solicitudes-compra/${duplicate.id}/confirmar-duplicidad`,
      () => d.getByRole('button', { name: 'Confirmar duplicidad', exact: true }).click(),
    );
    assert.equal(dup.duplicadaDeId, request.id);
    assert.equal(dup.estado, 'RECHAZADA');
    d = await purchaseDetail(p, 'supervisor', 'solicitudes', sku, request.id);
    await uiRequest(
      p,
      'POST',
      `solicitudes-compra/${request.id}/aprobar`,
      () => d.getByRole('button', { name: 'Aprobar', exact: true }).click(),
      400,
    );
    await fc(d, 'justificacion').fill('Compra mayor a S/5000 justificada para la revision E2E');
    const approved = await uiRequest(p, 'POST', `solicitudes-compra/${request.id}/aprobar`, () =>
      d.getByRole('button', { name: 'Aprobar', exact: true }).click(),
    );
    assert.equal(approved.estado, 'APROBADA');
    assert(approved.aprobadaEn && approved.enviadaEn);
    return { duplicidadConfirmada: true, umbralSinJustificacion: 400, aprobacionJustificada: 200 };
  });
  await step('F03-emision-y-recepciones-parcial-completa-sin-exceso', async () => {
    const c = pages.compras;
    let d = await purchaseDetail(c, 'compras', 'solicitudes', sku, request.id);
    order = await uiRequest(c, 'POST', `solicitudes-compra/${request.id}/emitir-orden`, () =>
      d.getByRole('button', { name: 'Emitir orden', exact: true }).click(),
    );
    const p = pages.almacen;
    const path = `ordenes-compra/${order.id}/recepciones`;
    const partial = await warehouseForm(
      p,
      'recepciones',
      { ordenId: order.id, cantidad: 4, motivo: 'Parcial E2E' },
      path,
      200,
    );
    let o = await api(p, 'GET', 'ordenes-compra/' + order.id);
    assert.equal(o.cantidadRecibida, 4);
    assert.equal(o.cantidadPendiente, 6);
    const retry = await api(p, 'POST', path, lastWrites[path]);
    assert.equal(retry.id, partial.id);
    assert.equal(await stock(p, product.id, wh1), 9);
    await warehouseForm(
      p,
      'recepciones',
      { ordenId: order.id, cantidad: 7, motivo: 'Exceso E2E' },
      path,
      0,
    );
    o = await api(p, 'GET', 'ordenes-compra/' + order.id);
    assert.equal(o.cantidadRecibida, 4);
    await warehouseForm(
      p,
      'recepciones',
      { ordenId: order.id, cantidad: 6, motivo: 'Completa E2E' },
      `ordenes-compra/${order.id}/recepciones`,
      200,
    );
    o = await api(p, 'GET', 'ordenes-compra/' + order.id);
    assert.equal(o.estado, 'ATENDIDA');
    assert.equal(await stock(p, product.id, wh1), 15);
    return {
      orden: order.id,
      parcial: 4,
      completa: 6,
      estado: 'ATENDIDA',
      saldo: 15,
      reintentoIdempotente: true,
      excesoBloqueadoSinEnvio: true,
    };
  });
  let stale, fresh, matching;
  await step('F04-conteo-obsoleto-rechazado-y-ajuste-fisico-confirmado', async () => {
    const a = pages.almacen,
      s = pages.supervisor,
      base = { productoId: product.id, almacenId: wh1, motivo: 'Conteo E2E' };
    stale = await warehouseForm(a, 'conteos', { ...base, cantidad: 13 }, 'conteos');
    await warehouseForm(a, 'movimientos', { ...base, tipo: 'ENTRADA', cantidad: 1 }, 'movimientos');
    async function detail(id) {
      await s.goto(cfg.base + '/supervisor/conteos');
      await fc(s, 'sku').fill(sku);
      await s
        .locator('form.ap-toolbar')
        .getByRole('button', { name: 'Consultar', exact: true })
        .click();
      await s.getByRole('button', { name: 'Ver detalle de ' + id, exact: true }).click();
    }
    await detail(stale.id);
    await uiRequest(
      s,
      'POST',
      `conteos/${stale.id}/confirmar`,
      () => s.getByRole('button', { name: 'Confirmar ajuste', exact: true }).click(),
      409,
    );
    assert.equal(await stock(a, product.id, wh1), 16);
    fresh = await warehouseForm(a, 'conteos', { ...base, cantidad: 13 }, 'conteos');
    await detail(fresh.id);
    const adjusted = await uiRequest(s, 'POST', `conteos/${fresh.id}/confirmar`, () =>
      s.getByRole('button', { name: 'Confirmar ajuste', exact: true }).click(),
    );
    assert(adjusted.confirmado);
    assert.equal(await stock(a, product.id, wh1), 13);
    matching = await warehouseForm(a, 'conteos', { ...base, cantidad: 13 }, 'conteos');
    return {
      conteoObsoleto: 409,
      saldoTrasRechazo: 16,
      saldoFisicoConfirmado: 13,
      conteosDelEscenario: 3,
    };
  });
  await step('F05-corte-indicadores-periodo-y-numeradores-verificables', async () => {
    const p = pages.supervisor;
    await p.goto(cfg.base + '/supervisor/cortes');
    const cut = await uiRequest(p, 'POST', 'cortes-stock', () =>
      p.getByRole('button', { name: 'Generar corte de stock', exact: true }).click(),
    );
    assert(cut.some((r) => r.sku === sku && r.almacenId === wh1 && r.cantidad === 13));
    await p.goto(cfg.base + '/supervisor/indicadores');
    await fc(p, 'sku').fill(sku);
    await fc(p, 'almacenId').selectOption(String(wh1));
    const pending = ['I05', 'I06', 'I07', 'I08', 'I09', 'I10'].map((code) =>
      p
        .waitForResponse(
          (r) =>
            new URL(r.url()).pathname === '/api/v1/indicadores' &&
            new URL(r.url()).searchParams.get('codigo') === code,
        )
        .then(async (r) => {
          assert.equal(r.status(), 200);
          return r.json();
        }),
    );
    await p
      .locator('form.ap-toolbar')
      .getByRole('button', { name: 'Consultar', exact: true })
      .click();
    const list = await Promise.all(pending);
    assert.equal(list.length, 6);
    const m = Object.fromEntries(list.map((x) => [x.codigo, x]));
    assert.equal(Number(m.I06.valor), 100);
    assert.equal(Number(m.I09.valor), 50);
    assert.equal(Number(m.I10.valor), 0);
    assert.equal(Number(m.I07.denominador), 3);
    assert.equal(Number(m.I07.numerador), 1);
    await shot(p, 'indicadores');
    return {
      lote: cut[0].lote,
      indicadores: list.map((x) => ({
        codigo: x.codigo,
        numerador: x.numerador,
        denominador: x.denominador,
        valor: x.valor,
        unidad: x.unidad,
      })),
    };
  });
  await step('consulta-comercial-ve-saldo-sin-acciones-operativas', async () => {
    const p = pages.consulta;
    await p.goto(cfg.base + '/consulta/existencias');
    await fc(p, 'productoId').selectOption(String(product.id));
    await fc(p, 'almacenId').selectOption(String(wh1));
    await uiRequest(p, 'GET', 'existencias', () =>
      p.locator('form.ap-toolbar').getByRole('button', { name: 'Consultar', exact: true }).click(),
    );
    assert((await p.locator('.ap-metric strong').innerText()).includes('13'));
    await api(p, 'GET', 'solicitudes-compra', undefined, 403);
    await p.goto(cfg.base + '/admin/usuarios');
    await p.waitForURL('**/acceso-denegado');
    return { saldo: 13, compras: 403, administracionDenegada: true };
  });
  await step('edicion-concurrente-409-y-revocacion-por-cambio-de-nombre', async () => {
    const c = cfg.cuentas.consulta,
      p2 = pages.admin,
      old = (await session(pages.consulta)).accessToken;
    await editUser(admin, c.username, async (d) => {
      await fc(d, 'username').fill(c.username + '-nuevo');
      await fc(d, 'motivo').fill('Cambio de nombre E2E');
    });
    await editUser(p2, c.username, async (d) => {
      await fc(d, 'motivo').fill('Formulario concurrente E2E');
    });
    await userSave(admin);
    await userSave(p2, 409);
    assert(await openDialog(p2).getByRole('alert').count());
    await api(pages.consulta, 'GET', 'auth/me', undefined, 401, old);
    c.username += '-nuevo';
    return { primeraEdicion: 200, formularioObsoleto: 409, tokenAnterior: 401 };
  });
  await step('restablecimiento-temporal-primer-acceso-e-historial', async () => {
    const c = cfg.cuentas.consulta;
    let d = await userDetail(admin, c.username);
    await d.getByRole('button', { name: 'Restablecer contraseña temporal', exact: true }).click();
    d = openDialog(admin);
    await fc(d, 'passwordTemporal').fill(cfg.temporalReset);
    await fc(d, 'confirmacionPassword').fill(cfg.temporalReset);
    await fc(d, 'motivo').fill('Recuperacion E2E');
    const [http] = await Promise.all([
      admin.waitForResponse(
        (r) =>
          r.request().method() === 'POST' &&
          /\/restablecer-password$/.test(new URL(r.url()).pathname),
      ),
      d.getByRole('button', { name: 'Guardar usuario', exact: true }).click(),
    ]);
    assert.equal(http.status(), 200);
    const p = await pageFor('recuperada');
    await login(p, c.username, cfg.temporalReset, true);
    await change(p, cfg.temporalReset, cfg.finalReset);
    c.nueva = cfg.finalReset;
    d = await userDetail(admin, c.username);
    const [events] = await Promise.all([
      admin.waitForResponse((r) => /\/eventos$/.test(new URL(r.url()).pathname)),
      d.getByRole('button', { name: 'Historial', exact: true }).click(),
    ]);
    const data = await events.json();
    assert(data.content.some((e) => e.tipo === 'RESTABLECIMIENTO_PASSWORD'));
    assert(!JSON.stringify(data).includes(cfg.temporalReset));
    return { reset: 200, cambioObligatorio: true, auditoriaSinClave: true };
  });
  await step('desactivacion-reactivacion-y-proteccion-del-administrador-propio', async () => {
    const c = cfg.cuentas.consulta,
      old = (await session(pages.recuperada)).accessToken;
    await editUser(admin, c.username, async (d) => {
      await fc(d, 'activo').uncheck();
      await fc(d, 'motivo').fill('Baja E2E');
    });
    await userSave(admin);
    const p = await pageFor('inactiva');
    await p.goto(cfg.base + '/login');
    await fc(p, 'username').fill(c.username);
    await fc(p, 'password').fill(c.nueva);
    await uiRequest(
      p,
      'POST',
      'auth/login',
      () => p.getByRole('button', { name: 'Iniciar sesión', exact: true }).click(),
      401,
    );
    await editUser(admin, c.username, async (d) => {
      await fc(d, 'activo').check();
      await fc(d, 'motivo').fill('Reactivacion E2E');
    });
    await userSave(admin);
    await api(pages.recuperada, 'GET', 'auth/me', undefined, 401, old);
    let d = await userDetail(admin, cfg.adminExistente.username);
    assert.equal(
      await d.getByRole('button', { name: 'Editar cuenta, rol y estado', exact: true }).count(),
      0,
    );
    assert.equal(
      await d.getByRole('button', { name: 'Restablecer contraseña temporal', exact: true }).count(),
      0,
    );
    return { loginInactiva: 401, tokenNoRevive: 401, cuentaPropiaProtegida: true };
  });
  await step('cambio-voluntario-desde-mi-cuenta-y-sesion-reemplazada', async () => {
    const p = pages.admin,
      c = cfg.cuentas.admin,
      old = (await session(p)).accessToken;
    await p.goto(cfg.base + '/admin');
    await p.getByRole('link', { name: 'Mi cuenta', exact: true }).click();
    await p.waitForURL('**/cuenta/cambiar-password');
    assert.equal(
      await p
        .getByText('Debes cambiar tu contraseña temporal antes de acceder a los módulos.', {
          exact: true,
        })
        .count(),
      0,
    );
    const changed = await change(p, c.nueva, c.nueva + '-voluntaria');
    assert.notEqual(changed.accessToken, old);
    await api(p, 'GET', 'auth/me', undefined, 401, old);
    return { cambioVoluntario: 200, tokenPrevio: 401, retornoAlPanel: true };
  });
  await step('cancelacion-del-pendiente-conserva-la-recepcion-registrada', async () => {
    const sku2 = ('REV-CANCEL-' + run).toUpperCase();
    await admin.goto(cfg.base + '/admin/productos');
    await admin.getByRole('button', { name: /\+ Nuevo producto/ }).click();
    let d = openDialog(admin);
    for (const [k, v] of Object.entries({
      sku: sku2,
      categoria: 'Pruebas',
      marca: 'Revision',
      modelo: 'Cancelacion',
      especificacion: 'Control de pendiente sin borrar recepciones',
    }))
      await fc(d, k).fill(v);
    const second = await uiRequest(
      admin,
      'POST',
      'productos',
      () => d.getByRole('button', { name: 'Guardar producto', exact: true }).click(),
      201,
    );
    const solicitud = await purchase(pages.compras, second.id, wh1, 'Pedido para cancelacion E2E');
    d = await purchaseDetail(pages.supervisor, 'supervisor', 'solicitudes', sku2, solicitud.id);
    await fc(d, 'justificacion').fill('Justificacion E2E de cancelacion');
    await uiRequest(pages.supervisor, 'POST', `solicitudes-compra/${solicitud.id}/aprobar`, () =>
      d.getByRole('button', { name: 'Aprobar', exact: true }).click(),
    );
    d = await purchaseDetail(pages.compras, 'compras', 'solicitudes', sku2, solicitud.id);
    const secondOrder = await uiRequest(
      pages.compras,
      'POST',
      `solicitudes-compra/${solicitud.id}/emitir-orden`,
      () => d.getByRole('button', { name: 'Emitir orden', exact: true }).click(),
    );
    await warehouseForm(
      pages.almacen,
      'recepciones',
      { ordenId: secondOrder.id, cantidad: 3, motivo: 'Parcial previo a cancelacion' },
      `ordenes-compra/${secondOrder.id}/recepciones`,
      200,
    );
    d = await purchaseDetail(
      pages.supervisor,
      'supervisor',
      'ordenes',
      sku2,
      secondOrder.numeroOrden,
    );
    await fc(d, 'motivo').fill('Cancelar solo las siete unidades pendientes');
    const canceled = await uiRequest(
      pages.supervisor,
      'POST',
      `ordenes-compra/${secondOrder.id}/cancelar`,
      () => d.getByRole('button', { name: 'Cancelar pendiente', exact: true }).click(),
    );
    assert.equal(canceled.estado, 'CANCELADA');
    assert.equal(canceled.cantidadRecibida, 3);
    assert.equal(await stock(pages.almacen, second.id, wh1), 3);
    await api(
      pages.almacen,
      'POST',
      `ordenes-compra/${secondOrder.id}/recepciones`,
      { cantidad: 1, motivo: 'Intento tras cancelacion', claveIdempotencia: crypto.randomUUID() },
      409,
    );
    return { recibidoConservado: 3, pendienteCancelado: 7, recepcionPosterior: 409 };
  });
  assert.equal(report.erroresNavegador.length, 0, 'Errores JavaScript no controlados en el navegador');
  report.exito = true;
  report.producto = { id: product.id, sku };
  save();
  console.log('VALIDACION UI COMPLETA: ' + report.casos.length + ' casos');
})()
  .catch((e) => {
    report.exito = false;
    save();
    console.error(scrub(e.message));
    process.exitCode = 1;
  })
  .finally(async () => {
    if (browser) await browser.close();
  });
