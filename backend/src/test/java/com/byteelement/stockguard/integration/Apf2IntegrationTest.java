package com.byteelement.stockguard.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.exception.*;
import com.byteelement.stockguard.repository.*;
import com.byteelement.stockguard.seguridad.*;
import com.byteelement.stockguard.service.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Apf2IntegrationTest {
  @Autowired ProductoRepository productos;
  @Autowired AlmacenRepository almacenes;
  @Autowired PoliticaStockRepository politicas;
  @Autowired MovimientoRepository movimientos;
  @Autowired InventarioService inventario;
  @Autowired ReporteService reportes;
  @Autowired UsuarioService usuarios;
  @Autowired MockMvc mvc;
  @Autowired JwtEncoder encoder;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean ActorActual actor;
  Producto p;
  Almacen a, b;
  Instant desde;
  static final String PASSWORD = "IntegrationPassword123!";

  @BeforeEach
  void preparar() {
    for (Rol rol : Rol.values()) {
      try {
        usuarios.crear(new CrearUsuarioRequest("test-" + rol.name(), PASSWORD, rol));
      } catch (ConflictoCatalogoException ignored) {
      }
    }
    autenticar("test-ALMACEN", Rol.ALMACEN);
    String codigo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    p =
        productos.saveAndFlush(
            new Producto("P-" + codigo, "RAM", "Marca", "Modelo", "Prueba", true));
    a = almacenes.saveAndFlush(new Almacen("A-" + codigo, "Origen"));
    b = almacenes.saveAndFlush(new Almacen("B-" + codigo, "Destino"));
    for (var almacen : almacenes.findAll())
      if (almacen.isActivo()) politicas.saveAndFlush(new PoliticaStock(p, almacen, 1, 10));
    desde = Instant.now().minusSeconds(1);
  }

  @AfterEach
  void limpiar() {
    SecurityContextHolder.clearContext();
    reset(actor);
  }

  static void autenticar(String nombre, Rol rol) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                nombre, "", List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
  }

  RegistrarMovimientoRequest mov(TipoMovimiento tipo, long cantidad, Almacen almacen) {
    return new RegistrarMovimientoRequest(
        p.getId(), almacen.getId(), tipo, cantidad, "Autor falsificado", "Prueba");
  }

  RegistrarTransferenciaRequest trans(long cantidad, Almacen origen, Almacen destino) {
    return new RegistrarTransferenciaRequest(
        p.getId(), origen.getId(), destino.getId(), cantidad, "Autor falsificado", "Transferencia");
  }

  String token(Rol rol) {
    return (String)
        usuarios.login(new LoginRequest("test-" + rol.name(), PASSWORD)).get("accessToken");
  }

  @Test
  void movimientosPersistenYAutorProvieneDeSesion() {
    var m = inventario.registrar(mov(TipoMovimiento.ENTRADA, 10, a));
    assertEquals("test-ALMACEN", m.autor());
    assertEquals(10, inventario.consultar(p.getId(), a.getId()).cantidad());
    assertEquals(
        10,
        jdbc.queryForObject(
            "select cantidad from existencia where producto_id=? and almacen_id=?",
            Long.class,
            p.getId(),
            a.getId()));
    assertEquals(m, inventario.obtenerMovimiento(m.id()));
  }

  @Test
  void transferenciaConfirmaAmbosSaldosYComparteReferencia() {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 10, a));
    var t = inventario.transferir(trans(4, a, b));
    assertEquals(6, t.saldoOrigenResultante());
    assertEquals(4, t.saldoDestinoResultante());
    assertEquals(
        movimientos.findById(t.movimientoSalidaId()).orElseThrow().getReferencia(),
        movimientos.findById(t.movimientoEntradaId()).orElseThrow().getReferencia());
  }

  @Test
  void salidaInsuficienteNoDejaCambios() {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 2, a));
    assertThrows(
        StockInsuficienteException.class,
        () -> inventario.registrar(mov(TipoMovimiento.SALIDA, 3, a)));
    assertEquals(2, inventario.consultar(p.getId(), a.getId()).cantidad());
    assertEquals(
        1,
        inventario
            .listarMovimientos(p.getId(), null, null, desde, Instant.now().plusSeconds(1), 0, 20)
            .getTotalElements());
  }

  @Test
  void falloDespuesDeGuardarPrimeraMitadRevierteTodaTransferencia() {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 10, a));
    doReturn("test-ALMACEN")
        .doThrow(new IllegalStateException("Fallo controlado en segundo movimiento"))
        .when(actor)
        .nombre();
    assertThrows(IllegalStateException.class, () -> inventario.transferir(trans(4, a, b)));
    reset(actor);
    assertEquals(10, inventario.consultar(p.getId(), a.getId()).cantidad());
    assertEquals(0, inventario.consultar(p.getId(), b.getId()).cantidad());
    assertEquals(
        1,
        inventario
            .listarMovimientos(p.getId(), null, null, desde, Instant.now().plusSeconds(1), 0, 20)
            .getTotalElements());
  }

  @Test
  void desbordamientoDestinoNoDescuentaOrigen() {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 1, a));
    inventario.registrar(mov(TipoMovimiento.ENTRADA, Long.MAX_VALUE, b));
    assertThrows(IllegalArgumentException.class, () -> inventario.transferir(trans(1, a, b)));
    assertEquals(1, inventario.consultar(p.getId(), a.getId()).cantidad());
    assertEquals(Long.MAX_VALUE, inventario.consultar(p.getId(), b.getId()).cantidad());
  }

  @Test
  void salidasConcurrentesNuncaDejanSaldoNegativo() throws Exception {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 5, a));
    try (var pool = Executors.newFixedThreadPool(12)) {
      CountDownLatch inicio = new CountDownLatch(1);
      List<Future<Boolean>> tareas = new ArrayList<>();
      for (int i = 0; i < 12; i++)
        tareas.add(
            pool.submit(
                () -> {
                  autenticar("test-ALMACEN", Rol.ALMACEN);
                  inicio.await();
                  try {
                    inventario.registrar(mov(TipoMovimiento.SALIDA, 1, a));
                    return true;
                  } catch (StockInsuficienteException e) {
                    return false;
                  } finally {
                    SecurityContextHolder.clearContext();
                  }
                }));
      inicio.countDown();
      int exitos = 0;
      for (var tarea : tareas) if (tarea.get(20, TimeUnit.SECONDS)) exitos++;
      assertEquals(5, exitos);
      assertEquals(0, inventario.consultar(p.getId(), a.getId()).cantidad());
    }
  }

  @Test
  void entradasConcurrentesCreanUnSoloSaldo() throws Exception {
    try (var pool = Executors.newFixedThreadPool(8)) {
      List<Future<?>> tareas = new ArrayList<>();
      CountDownLatch inicio = new CountDownLatch(1);
      for (int i = 0; i < 8; i++)
        tareas.add(
            pool.submit(
                () -> {
                  autenticar("test-ALMACEN", Rol.ALMACEN);
                  try {
                    inicio.await();
                    inventario.registrar(mov(TipoMovimiento.ENTRADA, 1, a));
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  } finally {
                    SecurityContextHolder.clearContext();
                  }
                }));
      inicio.countDown();
      for (var t : tareas) t.get(20, TimeUnit.SECONDS);
      assertEquals(8, inventario.consultar(p.getId(), a.getId()).cantidad());
      assertEquals(
          1,
          jdbc.queryForObject(
              "select count(*) from existencia where producto_id=? and almacen_id=?",
              Integer.class,
              p.getId(),
              a.getId()));
    }
  }

  @Test
  void transferenciasOpuestasConcurrentesConservanTotal() throws Exception {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 20, a));
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 20, b));
    try (var pool = Executors.newFixedThreadPool(8)) {
      List<Future<?>> tareas = new ArrayList<>();
      CountDownLatch inicio = new CountDownLatch(1);
      for (int i = 0; i < 8; i++) {
        final boolean ida = i % 2 == 0;
        tareas.add(
            pool.submit(
                () -> {
                  autenticar("test-ALMACEN", Rol.ALMACEN);
                  try {
                    inicio.await();
                    inventario.transferir(trans(1, ida ? a : b, ida ? b : a));
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  } finally {
                    SecurityContextHolder.clearContext();
                  }
                }));
      }
      inicio.countDown();
      for (var t : tareas) t.get(20, TimeUnit.SECONDS);
      assertEquals(20, inventario.consultar(p.getId(), a.getId()).cantidad());
      assertEquals(20, inventario.consultar(p.getId(), b.getId()).cantidad());
    }
  }

  @Test
  void jpqlFiltraSkuAlmacenPeriodoYPagina() {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 10, a));
    inventario.registrar(mov(TipoMovimiento.SALIDA, 2, a));
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 1, b));
    var page =
        inventario.listarMovimientos(
            null, p.getSku().toLowerCase(), a.getId(), desde, Instant.now().plusSeconds(1), 1, 1);
    assertEquals(2, page.getTotalElements());
    assertEquals(1, page.getContent().size());
    assertEquals(TipoMovimiento.SALIDA, page.getContent().getFirst().tipo());
    assertEquals(
        0,
        inventario
            .listarMovimientos(
                p.getId(),
                null,
                null,
                Instant.now().plusSeconds(2),
                Instant.now().plusSeconds(3),
                0,
                20)
            .getTotalElements());
  }

  @Test
  void indicadoresNoDuplicanProductosYPreservanPoliticaAlCorte() {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 15, a));
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 12, b));
    reportes.crearCorte();
    Instant hasta = Instant.now().plusSeconds(1);
    var exceso = reportes.indicador("I10", desde, hasta, p.getSku(), null);
    assertEquals(1, exceso.numerador());
    assertEquals(1, exceso.denominador());
    assertEquals(new BigDecimal("100.00"), exceso.valor());
    // Inicio del período con saldo cero se reconstruye desde los movimientos persistidos.
    var quiebre = reportes.indicador("I05", desde, hasta, p.getSku(), null);
    assertEquals(1, quiebre.numerador());
    var politica = politicas.findByProductoIdAndAlmacenId(p.getId(), a.getId()).orElseThrow();
    politica.actualizar(1, 100);
    politicas.saveAndFlush(politica);
    assertEquals(1, reportes.indicador("I10", desde, hasta, p.getSku(), a.getId()).numerador());
  }

  @Test
  void periodoSinDatosNoPresentaCeroComoMedicion() {
    var vacio = reportes.indicador("I07", desde, Instant.now().plusSeconds(1), p.getSku(), null);
    assertTrue(vacio.sinDatos());
    assertNull(vacio.valor());
    assertEquals(0, vacio.denominador());
    assertThrows(
        IllegalArgumentException.class,
        () -> reportes.indicador("I06", desde, Instant.now().plusSeconds(1), null, null));
  }

  @Test
  void conteoCalculaCoincidenciaDelSaldoAlCorteYAjusteEsTrazable() {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 10, a));
    var c = inventario.contar(new RegistrarConteoRequest(p.getId(), a.getId(), 10L, "Coincide"));
    var diferente =
        inventario.contar(new RegistrarConteoRequest(p.getId(), b.getId(), 3L, "Diferencia"));
    var i = reportes.indicador("I07", desde, Instant.now().plusSeconds(1), p.getSku(), null);
    assertEquals(1, i.numerador());
    assertEquals(2, i.denominador());
    assertEquals(new BigDecimal("50.00"), i.valor());
    autenticar("test-SUPERVISOR", Rol.SUPERVISOR);
    var ajuste = inventario.confirmarConteo(diferente.id());
    assertTrue(ajuste.confirmado());
    assertNotNull(ajuste.movimientoId());
    assertEquals(3, inventario.consultar(p.getId(), b.getId()).cantidad());
    assertThrows(
        ConflictoCatalogoException.class, () -> inventario.confirmarConteo(diferente.id()));
    assertEquals(10, inventario.obtenerConteo(c.id()).saldoCorte());
  }

  @Test
  void conteoObsoletoRechazaInclusoSiSaldoVuelveAlMismoValor() {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 10, a));
    var c = inventario.contar(new RegistrarConteoRequest(p.getId(), a.getId(), 8L, "Conteo"));
    inventario.registrar(mov(TipoMovimiento.SALIDA, 1, a));
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 1, a));
    assertThrows(ConflictoCatalogoException.class, () -> inventario.confirmarConteo(c.id()));
  }

  @Test
  void rotacionUsaConsumoComercialYPromedioTemporal() {
    inventario.registrar(mov(TipoMovimiento.ENTRADA, 10, a));
    inventario.registrar(mov(TipoMovimiento.SALIDA, 4, a));
    inventario.transferir(trans(2, a, b));
    var r =
        reportes
            .rotacion(p.getSku(), a.getId(), desde, Instant.now().plusSeconds(1), 0, 20)
            .getContent()
            .getFirst();
    assertEquals(4L, r.get("consumo"));
    assertNotNull(r.get("rotacion"));
  }

  @Test
  void loginRealYTokenAusenteInvalidoExpiradoSonRechazados() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-ALMACEN\",\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString());
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test-ALMACEN\",\"password\":\"incorrecta\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/productos")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());
    var claims =
        JwtClaimsSet.builder()
            .issuer("stockguard")
            .subject("test-ALMACEN")
            .issuedAt(Instant.now().minusSeconds(7200))
            .expiresAt(Instant.now().minusSeconds(3600))
            .claim("rol", "ALMACEN")
            .build();
    String expirado =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer " + expirado))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void permisosSeAplicanPorRolEnServidor() throws Exception {
    for (Rol rol : Rol.values())
      mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer " + token(rol)))
          .andExpect(status().isOk());
    String cuerpo =
        "{\"productoId\":"
            + p.getId()
            + ",\"almacenId\":"
            + a.getId()
            + ",\"tipo\":\"ENTRADA\",\"cantidad\":1,\"motivo\":\"Prueba\"}";
    for (Rol rol : Rol.values()) {
      var result =
          mvc.perform(
              post("/api/v1/movimientos")
                  .header("Authorization", "Bearer " + token(rol))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(cuerpo));
      if (rol == Rol.ALMACEN)
        result.andExpect(status().isCreated()).andExpect(jsonPath("$.autor").value("test-ALMACEN"));
      else result.andExpect(status().isForbidden());
    }
    mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + token(Rol.ALMACEN)))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/usuarios").header("Authorization", "Bearer " + token(Rol.ADMINISTRADOR)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    mvc.perform(
            put("/api/v1/productos/" + p.getId() + "/almacenes/" + a.getId() + "/politica-stock")
                .header("Authorization", "Bearer " + token(Rol.ADMINISTRADOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"minimo\":1,\"maximo\":20}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/v1/productos/" + p.getId() + "/almacenes/" + a.getId() + "/politica-stock")
                .header("Authorization", "Bearer " + token(Rol.SUPERVISOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"minimo\":1,\"maximo\":20}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/conteos/123/confirmar")
                .header("Authorization", "Bearer " + token(Rol.ALMACEN)))
        .andExpect(status().isForbidden());
  }

  @Test
  void contratoPaginacionYFechasInvalidasDevuelve400() throws Exception {
    mvc.perform(
            get("/api/v1/movimientos")
                .param("tamanio", "0")
                .header("Authorization", "Bearer " + token(Rol.ALMACEN)))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/indicadores")
                .param("codigo", "I07")
                .param("desde", "2026-10-11T00:00:00Z")
                .param("hasta", "2026-10-10T00:00:00Z")
                .header("Authorization", "Bearer " + token(Rol.SUPERVISOR)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void todosLosContratosRestringidosRechazanTokenAusenteInvalidoYRolIndebido() throws Exception {
    record Caso(String metodo, String ruta, Rol indebido) {}
    var casos =
        List.of(
            new Caso("POST", "/api/v1/productos", Rol.ALMACEN),
            new Caso("PUT", "/api/v1/productos/" + p.getId(), Rol.ALMACEN),
            new Caso("PATCH", "/api/v1/productos/" + p.getId() + "/desactivar", Rol.ALMACEN),
            new Caso("POST", "/api/v1/almacenes", Rol.COMPRAS),
            new Caso("PUT", "/api/v1/almacenes/" + a.getId(), Rol.ALMACEN),
            new Caso("PATCH", "/api/v1/almacenes/" + a.getId() + "/desactivar", Rol.SUPERVISOR),
            new Caso(
                "PUT",
                "/api/v1/productos/" + p.getId() + "/almacenes/" + a.getId() + "/politica-stock",
                Rol.ADMINISTRADOR),
            new Caso("POST", "/api/v1/movimientos", Rol.COMPRAS),
            new Caso("POST", "/api/v1/transferencias", Rol.SUPERVISOR),
            new Caso("POST", "/api/v1/conteos", Rol.ADMINISTRADOR),
            new Caso("POST", "/api/v1/conteos/1/confirmar", Rol.ALMACEN),
            new Caso("GET", "/api/v1/conteos/1", Rol.COMPRAS),
            new Caso("POST", "/api/v1/cortes-stock", Rol.ADMINISTRADOR),
            new Caso("GET", "/api/v1/cortes-stock/lote", Rol.ALMACEN),
            new Caso("GET", "/api/v1/indicadores", Rol.ALMACEN),
            new Caso("POST", "/api/v1/usuarios", Rol.SUPERVISOR),
            new Caso("GET", "/api/v1/usuarios", Rol.COMPRAS));
    for (var c : casos) {
      mvc.perform(request(org.springframework.http.HttpMethod.valueOf(c.metodo()), c.ruta()))
          .andExpect(status().isUnauthorized());
      mvc.perform(
              request(org.springframework.http.HttpMethod.valueOf(c.metodo()), c.ruta())
                  .header("Authorization", "Bearer invalido"))
          .andExpect(status().isUnauthorized());
      mvc.perform(
              request(org.springframework.http.HttpMethod.valueOf(c.metodo()), c.ruta())
                  .header("Authorization", "Bearer " + token(c.indebido())))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void operacionesSeRelacionanConUsuarioAutenticadoYSupervisor() {
    long almacenId =
        jdbc.queryForObject("select id from usuario where username='test-ALMACEN'", Long.class);
    long supervisorId =
        jdbc.queryForObject("select id from usuario where username='test-SUPERVISOR'", Long.class);
    var movimiento = inventario.registrar(mov(TipoMovimiento.ENTRADA, 10, a));
    assertEquals(
        almacenId,
        jdbc.queryForObject(
            "select usuario_id from movimiento where id=?", Long.class, movimiento.id()));
    var c = inventario.contar(new RegistrarConteoRequest(p.getId(), a.getId(), 8L, "Diferencia"));
    assertEquals(
        almacenId,
        jdbc.queryForObject("select usuario_id from conteo where id=?", Long.class, c.id()));
    assertNull(
        jdbc.queryForObject("select confirmado_por_id from conteo where id=?", Long.class, c.id()));
    autenticar("test-SUPERVISOR", Rol.SUPERVISOR);
    var confirmado = inventario.confirmarConteo(c.id());
    assertEquals(
        supervisorId,
        jdbc.queryForObject("select confirmado_por_id from conteo where id=?", Long.class, c.id()));
    assertEquals(
        supervisorId,
        jdbc.queryForObject(
            "select usuario_id from movimiento where id=?", Long.class, confirmado.movimientoId()));
    var corte = reportes.crearCorte().getFirst();
    assertEquals(
        supervisorId,
        jdbc.queryForObject(
            "select usuario_id from corte_stock where id=?", Long.class, corte.id()));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> jdbc.update("delete from usuario where id=?", almacenId));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            jdbc.update("update movimiento set usuario_id=999999999 where id=?", movimiento.id()));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> jdbc.update("update movimiento set usuario_id=null where id=?", movimiento.id()));
  }

  @Test
  void identidadReferencialSeConservaAlCambiarNombreDelUsuario() {
    var movimiento = inventario.registrar(mov(TipoMovimiento.ENTRADA, 3, a));
    long usuarioId =
        jdbc.queryForObject(
            "select usuario_id from movimiento where id=?", Long.class, movimiento.id());
    try {
      jdbc.update("update usuario set username='usuario-renombrado' where id=?", usuarioId);
      assertEquals(
          usuarioId,
          jdbc.queryForObject(
              "select usuario_id from movimiento where id=?", Long.class, movimiento.id()));
      assertEquals("test-ALMACEN", inventario.obtenerMovimiento(movimiento.id()).autor());
    } finally {
      jdbc.update("update usuario set username='test-ALMACEN' where id=?", usuarioId);
    }
  }

  @Test
  void actorSinCuentaPersistidaNoPuedeCrearHistorial() {
    autenticar("actor-sin-cuenta", Rol.ALMACEN);
    assertThrows(
        org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
            .class,
        () -> inventario.registrar(mov(TipoMovimiento.ENTRADA, 5, a)));
    assertEquals(0, inventario.consultar(p.getId(), a.getId()).cantidad());
    assertEquals(
        0,
        inventario
            .listarMovimientos(p.getId(), null, null, desde, Instant.now().plusSeconds(1), 0, 20)
            .getTotalElements());
  }
}
