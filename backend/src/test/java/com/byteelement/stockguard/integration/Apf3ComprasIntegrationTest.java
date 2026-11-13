package com.byteelement.stockguard.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.dto.ComprasDtos.*;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.exception.ConflictoCatalogoException;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Apf3ComprasIntegrationTest {
  @Autowired ComprasService compras;
  @Autowired ReposicionService reposicion;
  @Autowired ReporteService reportes;
  @Autowired InventarioService inventario;
  @Autowired UsuarioService usuarios;
  @Autowired UsuarioRepository users;
  @Autowired ProductoRepository productos;
  @Autowired AlmacenRepository almacenes;
  @Autowired PoliticaStockRepository politicas;
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  Producto p;
  Almacen a;
  Instant desde;
  static final String PASSWORD = "IntegrationPassword123!";

  @BeforeEach
  void preparar() {
    for (var rol : Rol.values())
      if (users.findByUsername("apf3-" + rol).isEmpty())
        usuarios.crear(new CrearUsuarioRequest("apf3-" + rol, PASSWORD, rol));
    String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    p = productos.saveAndFlush(new Producto("APF3-" + id, "RAM", "Marca", "Modelo", "8GB", true));
    a = almacenes.saveAndFlush(new Almacen("APF3-" + id, "Prueba"));
    politicas.saveAndFlush(new PoliticaStock(p, a, 10, 100));
    desde = Instant.now().minusSeconds(2);
    login(Rol.COMPRAS);
  }

  @AfterEach
  void salir() {
    SecurityContextHolder.clearContext();
  }

  void login(Rol r) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "apf3-" + r, "", List.of(new SimpleGrantedAuthority("ROLE_" + r))));
  }

  CrearSolicitud nueva(String clave, long cantidad, boolean urgente, BigDecimal monto) {
    return new CrearSolicitud(
        p.getId(),
        a.getId(),
        cantidad,
        "Reposicion",
        monto,
        p.getSku() + "-" + clave,
        urgente,
        urgente ? "Quiebre actual" : null);
  }

  Solicitud crear(long c, boolean urgente) {
    return compras.crear(nueva(UUID.randomUUID().toString(), c, urgente, new BigDecimal("100")));
  }

  Orden orden(long c) {
    login(Rol.COMPRAS);
    var s = crear(c, false);
    login(Rol.SUPERVISOR);
    compras.aprobar(s.id(), new Aprobar(null));
    login(Rol.COMPRAS);
    return compras.emitir(s.id());
  }

  @Test
  void RF04_idempotenciaYSemejanzaNoSonDuplicidadConfirmada() {
    var r = nueva("misma-clave", 10, false, new BigDecimal("100"));
    var s = compras.crear(r);
    assertEquals(s.id(), compras.crear(r).id());
    assertThrows(
        ConflictoCatalogoException.class,
        () -> compras.crear(nueva("misma-clave", 11, false, new BigDecimal("100"))));
    var adicional = crear(10, false);
    assertTrue(adicional.similaresAbiertas().contains(s.id()));
    var i = reportes.indicador("I09", desde, Instant.now().plusSeconds(1), p.getSku(), a.getId());
    assertEquals(2, i.denominador());
    assertEquals(0, i.numerador());
    login(Rol.SUPERVISOR);
    var duplicada =
        compras.duplicada(adicional.id(), new Duplicidad(s.id(), "Misma necesidad confirmada"));
    assertEquals("RECHAZADA", duplicada.estado());
    assertNotNull(duplicada.duplicidadConfirmadaEn());
    assertNull(duplicada.aprobadaEn());
    assertNotNull(duplicada.revisadaEn());
    assertThrows(
        ConflictoCatalogoException.class, () -> compras.aprobar(adicional.id(), new Aprobar(null)));
    assertEquals(
        new BigDecimal("50.00"),
        reportes
            .indicador("I09", desde, Instant.now().plusSeconds(1), p.getSku(), a.getId())
            .valor());
  }

  @Test
  void RF05_montoUrgenciaYUnaSolaDecision() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            compras.crear(
                new CrearSolicitud(
                    p.getId(), a.getId(), 1L, "Prueba", BigDecimal.TEN, "urgencia", true, null)));
    var s = compras.crear(nueva("alto", 10, true, new BigDecimal("6000")));
    login(Rol.SUPERVISOR);
    assertThrows(IllegalArgumentException.class, () -> compras.aprobar(s.id(), new Aprobar(null)));
    var aprobada = compras.aprobar(s.id(), new Aprobar("Reposicion critica autorizada"));
    assertNotNull(aprobada.aprobadaEn());
    assertThrows(
        ConflictoCatalogoException.class,
        () -> compras.rechazar(s.id(), new Motivo("Intento repetido")));
    login(Rol.COMPRAS);
    var o = compras.emitir(s.id());
    assertEquals(o.id(), compras.emitir(s.id()).id());
    assertTrue(o.urgente());
    assertEquals(new BigDecimal("6000.00"), o.montoEstimado());
  }

  @Test
  void RF02_RF05_recepcionParcialIdempotenteYSinExceso() {
    var o = orden(10);
    login(Rol.ALMACEN);
    var r = new Recibir(4L, "Recepcion parcial", "parcial");
    var x = compras.recibir(o.id(), r);
    assertEquals(x.id(), compras.recibir(o.id(), r).id());
    assertEquals(4, inventario.consultar(p.getId(), a.getId()).cantidad());
    assertEquals(6, compras.obtenerOrden(o.id()).cantidadPendiente());
    assertThrows(
        ConflictoCatalogoException.class,
        () -> compras.recibir(o.id(), new Recibir(7L, "Exceso", "exceso")));
    assertEquals(4, inventario.consultar(p.getId(), a.getId()).cantidad());
    compras.recibir(o.id(), new Recibir(6L, "Final", "final"));
    assertEquals("ATENDIDA", compras.obtenerOrden(o.id()).estado());
    assertEquals(10, inventario.consultar(p.getId(), a.getId()).cantidad());
    assertThrows(
        ConflictoCatalogoException.class,
        () -> compras.recibir(o.id(), new Recibir(1L, "Extra", "extra")));
  }

  @Test
  void RF05_cancelarNoRevierteLoYaRecibido() {
    var o = orden(10);
    login(Rol.ALMACEN);
    compras.recibir(o.id(), new Recibir(3L, "Parcial", "r"));
    login(Rol.SUPERVISOR);
    var cancelada = compras.cancelar(o.id(), new Motivo("Cancelar saldo"));
    assertEquals(0, cancelada.cantidadPendiente());
    assertEquals(3, cancelada.cantidadRecibida());
    assertEquals(3, inventario.consultar(p.getId(), a.getId()).cantidad());
    login(Rol.ALMACEN);
    assertThrows(
        ConflictoCatalogoException.class,
        () -> compras.recibir(o.id(), new Recibir(1L, "Tardia", "t")));
  }

  @Test
  void RF03_minMaxConsumoYPendientesConResultadoConocido() {
    login(Rol.ALMACEN);
    inventario.registrar(
        new RegistrarMovimientoRequest(
            p.getId(), a.getId(), TipoMovimiento.ENTRADA, 132L, null, "Inicial"));
    inventario.registrar(
        new RegistrarMovimientoRequest(
            p.getId(), a.getId(), TipoMovimiento.SALIDA, 120L, null, "Consumo"));
    orden(20);
    var r = reposicion.listar(p.getSku(), a.getId(), 0, 20).getContent().getFirst();
    assertEquals(12, r.stock());
    assertEquals(120, r.consumo());
    assertEquals(20, r.pedidosPendientes());
    assertEquals(40, r.puntoReposicion());
    assertEquals(68, r.cantidadSugerida());
    assertTrue(r.urgenciaSugerida());
    assertEquals(1, reposicion.listar(p.getSku(), a.getId(), 0, 20).getTotalElements());
  }

  @Test
  void RF08_I06_I08_I09FiltranPeriodoYDenominador() {
    var urgente = crear(1, true);
    login(Rol.SUPERVISOR);
    compras.aprobar(urgente.id(), new Aprobar(null));
    login(Rol.COMPRAS);
    compras.emitir(urgente.id());
    orden(2);
    Instant fin = Instant.now().plusSeconds(1);
    assertEquals(
        new BigDecimal("50.00"),
        reportes.indicador("I06", desde, fin, p.getSku(), a.getId()).valor());
    // Dos tiempos exactos conocidos: 1 dia y 3 dias. Filtrados por fecha de aprobacion.
    Instant aprobacion = Instant.now();
    jdbc.update(
        "update solicitud_compra set enviada_en=?,aprobada_en=? where id=?",
        java.sql.Timestamp.from(aprobacion.minusSeconds(86400)),
        java.sql.Timestamp.from(aprobacion),
        urgente.id());
    Long otra =
        jdbc.queryForObject(
            "select id from solicitud_compra where producto_id=? and id<>?",
            Long.class,
            p.getId(),
            urgente.id());
    jdbc.update(
        "update solicitud_compra set enviada_en=?,aprobada_en=? where id=?",
        java.sql.Timestamp.from(aprobacion.minusSeconds(259200)),
        java.sql.Timestamp.from(aprobacion),
        otra);
    var i = reportes.indicador("I08", desde, fin, p.getSku(), a.getId());
    assertEquals("dias", i.unidad());
    assertEquals(2, i.denominador());
    assertEquals(345600, i.numerador());
    assertEquals(new BigDecimal("2.0000"), i.valor());
    for (String codigo : List.of("I06", "I08", "I09")) {
      var vacio = reportes.indicador(codigo, fin, fin.plusSeconds(60), p.getSku(), a.getId());
      assertTrue(vacio.sinDatos());
      assertNull(vacio.valor());
    }
  }

  @Test
  void RF09_contratosNuevosRechazanSinTokenYRolIndebido() throws Exception {
    String token =
        (String)
            usuarios
                .login(new LoginRequest("apf3-CONSULTA_COMERCIAL", PASSWORD))
                .get("accessToken");
    for (String ruta :
        List.of(
            "/api/v1/solicitudes-compra",
            "/api/v1/ordenes-compra",
            "/api/v1/reposicion",
            "/api/v1/evidencias")) {
      mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
      mvc.perform(get(ruta).header("Authorization", "Bearer " + token))
          .andExpect(status().isForbidden());
    }
    login(Rol.ALMACEN);
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class, () -> crear(1, false));
  }

  @Test
  void RF05_emisionesConcurrentesCreanUnaOrden() throws Exception {
    var s = crear(10, false);
    login(Rol.SUPERVISOR);
    compras.aprobar(s.id(), new Aprobar(null));
    var pool = Executors.newFixedThreadPool(4);
    try {
      var tareas = new ArrayList<Callable<Long>>();
      for (int n = 0; n < 8; n++)
        tareas.add(
            () -> {
              try {
                login(Rol.COMPRAS);
                return compras.emitir(s.id()).id();
              } finally {
                salir();
              }
            });
      var ids = new HashSet<Long>();
      for (var f : pool.invokeAll(tareas)) ids.add(f.get(20, TimeUnit.SECONDS));
      assertEquals(1, ids.size());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void RF05_recepcionesConcurrentesNoSuperanLoOrdenado() throws Exception {
    var o = orden(5);
    var pool = Executors.newFixedThreadPool(4);
    try {
      var tareas = new ArrayList<Callable<Boolean>>();
      for (int n = 0; n < 10; n++) {
        final int clave = n;
        tareas.add(
            () -> {
              try {
                login(Rol.ALMACEN);
                compras.recibir(o.id(), new Recibir(1L, "Entrega", "r-" + clave));
                return true;
              } catch (ConflictoCatalogoException e) {
                return false;
              } finally {
                salir();
              }
            });
      }
      int aceptadas = 0;
      for (var f : pool.invokeAll(tareas)) if (f.get(20, TimeUnit.SECONDS)) aceptadas++;
      assertEquals(5, aceptadas);
      assertEquals(5, inventario.consultar(p.getId(), a.getId()).cantidad());
      assertEquals(5, compras.obtenerOrden(o.id()).cantidadRecibida());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void RF10_flujoComprasHttpYDashboardConPermisos() throws Exception {
    var json = tools.jackson.databind.json.JsonMapper.builder().build();
    String tokenCompras =
        (String) usuarios.login(new LoginRequest("apf3-COMPRAS", PASSWORD)).get("accessToken");
    String tokenSupervisor =
        (String) usuarios.login(new LoginRequest("apf3-SUPERVISOR", PASSWORD)).get("accessToken");
    String tokenAlmacen =
        (String) usuarios.login(new LoginRequest("apf3-ALMACEN", PASSWORD)).get("accessToken");
    var result =
        mvc.perform(
                post("/api/v1/solicitudes-compra")
                    .header("Authorization", "Bearer " + tokenCompras)
                    .contentType("application/json")
                    .content(json.writeValueAsString(nueva("http", 5, false, BigDecimal.TEN))))
            .andExpect(status().isOk())
            .andReturn();
    Long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(
            post("/api/v1/solicitudes-compra/" + id + "/aprobar")
                .header("Authorization", "Bearer " + tokenSupervisor)
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isOk());
    result =
        mvc.perform(
                post("/api/v1/solicitudes-compra/" + id + "/emitir-orden")
                    .header("Authorization", "Bearer " + tokenCompras))
            .andExpect(status().isOk())
            .andReturn();
    Long ordenId = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(
            post("/api/v1/ordenes-compra/" + ordenId + "/recepciones")
                .header("Authorization", "Bearer " + tokenAlmacen)
                .contentType("application/json")
                .content(
                    "{\"cantidad\":5,\"motivo\":\"Llegada\",\"claveIdempotencia\":\"http-r\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.movimientoId").isNumber());
    mvc.perform(
            get("/api/v1/reposicion")
                .param("sku", p.getSku())
                .header("Authorization", "Bearer " + tokenCompras))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].stock").value(5));
    mvc.perform(post("/api/v1/cortes-stock").header("Authorization", "Bearer " + tokenSupervisor))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/dashboard")
                .param("desde", desde.toString())
                .param("hasta", Instant.now().plusSeconds(1).toString())
                .param("sku", p.getSku())
                .param("almacenId", a.getId().toString())
                .header("Authorization", "Bearer " + tokenSupervisor))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(6));
    for (String ruta :
        List.of(
            "/api/v1/solicitudes-compra/1/aprobar",
            "/api/v1/solicitudes-compra/1/rechazar",
            "/api/v1/solicitudes-compra/1/confirmar-duplicidad",
            "/api/v1/solicitudes-compra/1/emitir-orden",
            "/api/v1/ordenes-compra/1/recepciones",
            "/api/v1/ordenes-compra/1/cancelar")) {
      mvc.perform(post(ruta)).andExpect(status().isUnauthorized());
      mvc.perform(post(ruta).header("Authorization", "Bearer invalido"))
          .andExpect(status().isUnauthorized());
      String indebido =
          ruta.endsWith("emitir-orden") || ruta.endsWith("recepciones")
              ? tokenSupervisor
              : tokenCompras;
      mvc.perform(post(ruta).header("Authorization", "Bearer " + indebido))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void RF05_rechazoNoContaminaTiempoAprobacion() {
    var s = crear(1, false);
    login(Rol.SUPERVISOR);
    var r = compras.rechazar(s.id(), new Motivo("No necesario"));
    assertNull(r.aprobadaEn());
    assertNotNull(r.revisadaEn());
    assertTrue(
        reportes
            .indicador("I08", desde, Instant.now().plusSeconds(1), p.getSku(), a.getId())
            .sinDatos());
    login(Rol.COMPRAS);
    assertThrows(ConflictoCatalogoException.class, () -> compras.emitir(s.id()));
  }
}
