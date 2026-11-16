package com.byteelement.stockguard.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.repository.*;
import com.byteelement.stockguard.seguridad.*;
import com.byteelement.stockguard.service.InstalacionCuentasService;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@SpringBootTest(
    properties = {
      "spring.datasource.url=${INSTALACION_TEST_DB_URL:jdbc:h2:mem:instalacion-cuentas;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000}",
      "stockguard.bootstrap.enabled=false"
    })
class InstalacionCuentasIntegrationTest {
  @Autowired org.springframework.test.web.servlet.MockMvc mvc;
  @Autowired com.byteelement.stockguard.service.UsuarioService servicioUsuarios;
  @Autowired tools.jackson.databind.ObjectMapper json;
  @Autowired InstalacionCuentasService instalacion;
  @Autowired CuentasInicialesProperties config;
  @Autowired UsuarioRepository usuarios;
  @Autowired ControlCuentasRepository control;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder passwords;
  @MockitoSpyBean EventoUsuarioRepository eventos;

  @BeforeEach
  void vaciarBaseExclusiva() {
    // Esta clase necesita una base propia: nunca limpiar una base de operacion o del resto de
    // suites.
    String url =
        jdbc.execute(
            (org.springframework.jdbc.core.ConnectionCallback<String>)
                c -> c.getMetaData().getURL());
    if (url.startsWith("jdbc:postgresql:")) {
      String base =
          url.substring(url.lastIndexOf('/') + 1)
              .split("\\?")[0]
              .toLowerCase(java.util.Locale.ROOT);
      assertTrue(
          base.contains("test") || base.contains("prueba"),
          "La base de instalacion debe ser exclusiva de pruebas");
      for (String otra :
          List.of(
              System.getenv().getOrDefault("TEST_DB_URL", ""),
              System.getenv().getOrDefault("DEMO_TEST_DB_URL", "")))
        assertNotEquals(
            url.split("\\?")[0],
            otra.split("\\?")[0],
            "INSTALACION_TEST_DB_URL debe ser distinta de TEST_DB_URL y DEMO_TEST_DB_URL");
    }
    assertEquals(
        0,
        jdbc.queryForObject("select count(*) from producto", Long.class),
        "La base de instalacion no debe contener inventario");
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from usuario where username not like 'inst-%'", Long.class),
        "La base contiene usuarios ajenos a esta prueba; no se modifica");
    jdbc.update("delete from evento_usuario");
    jdbc.update("delete from usuario");
    jdbc.update("update control_cuentas set instalacion_completa=false where id=1");
    config.setEnabled(true);
    config.setUsername("inst-admin");
    config.setPassword("InstallAdminPassword123!");
    config.getAlmacen().setUsername("inst-almacen");
    config.getAlmacen().setPassword("InstallWarehousePassword123!");
    config.getCompras().setUsername("inst-compras");
    config.getCompras().setPassword("InstallPurchasingPassword123!");
    config.getSupervisor().setUsername("inst-supervisor");
    config.getSupervisor().setPassword("InstallSupervisorPassword123!");
    config.getConsulta().setUsername("inst-consulta");
    config.getConsulta().setPassword("InstallConsultationPassword123!");
  }

  @AfterEach
  void limpiarSpy() {
    reset(eventos);
    config.setEnabled(false);
  }

  private String completar(String nombre, String inicial) throws Exception {
    String token =
        (String)
            servicioUsuarios
                .login(new com.byteelement.stockguard.dto.LoginRequest(nombre, inicial))
                .get("accessToken");
    var respuesta =
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/v1/auth/cambiar-password")
                    .header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "passwordActual",
                                inicial,
                                "passwordNueva",
                                "FinalInstallPassword789!",
                                "confirmacionPassword",
                                "FinalInstallPassword789!"))))
            .andReturn()
            .getResponse();
    assertEquals(200, respuesta.getStatus());
    return json.readTree(respuesta.getContentAsString()).get("accessToken").asString();
  }

  @Test
  void dosAdministradoresNoPuedenDesactivarseMutuamenteDejandoCeroActivos() throws Exception {
    instalacion.inicializar();
    String tokenA = completar("inst-admin", "InstallAdminPassword123!");
    servicioUsuarios.crear(
        new com.byteelement.stockguard.dto.CrearUsuarioRequest(
            "inst-admin-b", "SecondAdminPassword123!", Rol.ADMINISTRADOR));
    String tokenB = completar("inst-admin-b", "SecondAdminPassword123!");
    var a = usuarios.findByUsername("inst-admin").orElseThrow();
    var b = usuarios.findByUsername("inst-admin-b").orElseThrow();
    var pool = Executors.newFixedThreadPool(2);
    try {
      List<Callable<Integer>> operaciones =
          List.of(
              () ->
                  mvc.perform(
                          org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                  "/api/v1/usuarios/" + b.getId())
                              .header("Authorization", "Bearer " + tokenA)
                              .contentType("application/json")
                              .content(
                                  json.writeValueAsString(
                                      Map.of(
                                          "username",
                                          b.getUsername(),
                                          "rol",
                                          "ADMINISTRADOR",
                                          "activo",
                                          false,
                                          "version",
                                          b.getVersion(),
                                          "motivo",
                                          "Prueba concurrente"))))
                      .andReturn()
                      .getResponse()
                      .getStatus(),
              () ->
                  mvc.perform(
                          org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                  "/api/v1/usuarios/" + a.getId())
                              .header("Authorization", "Bearer " + tokenB)
                              .contentType("application/json")
                              .content(
                                  json.writeValueAsString(
                                      Map.of(
                                          "username",
                                          a.getUsername(),
                                          "rol",
                                          "ADMINISTRADOR",
                                          "activo",
                                          false,
                                          "version",
                                          a.getVersion(),
                                          "motivo",
                                          "Prueba concurrente"))))
                      .andReturn()
                      .getResponse()
                      .getStatus());
      var estados = new ArrayList<Integer>();
      for (var f : pool.invokeAll(operaciones)) estados.add(f.get(20, TimeUnit.SECONDS));
      Collections.sort(estados);
      assertEquals(List.of(200, 401), estados);
      assertEquals(1, usuarios.countByRolAndActivoTrue(Rol.ADMINISTRADOR));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void creaCincoCuentasDistintasSinDatosDemoYSinGuardarPasswordsEnClaro() {
    assertTrue(instalacion.inicializar());
    assertEquals(5, usuarios.count());
    assertEquals(5, eventos.count());
    for (var e : config.cuentas().entrySet()) {
      var u = usuarios.findByUsername(e.getValue().getUsername()).orElseThrow();
      assertEquals(e.getKey(), u.getRol());
      assertTrue(u.isActivo());
      assertTrue(u.isRequiereCambioPassword());
      assertNotEquals(e.getValue().getPassword(), u.getPasswordHash());
      assertTrue(passwords.matches(e.getValue().getPassword(), u.getPasswordHash()));
    }
    assertTrue(control.findById(1L).orElseThrow().isInstalacionCompleta());
    assertEquals(0, jdbc.queryForObject("select count(*) from producto", Long.class));
  }

  @Test
  void reiniciarNoCambiaPasswordsNiReactivaCuentas() {
    instalacion.inicializar();
    String hash = usuarios.findByUsername("inst-admin").orElseThrow().getPasswordHash();
    jdbc.update("update usuario set activo=false where username='inst-almacen'");
    config.setPassword("OtroPasswordQueNoSeAplica456!");
    assertFalse(instalacion.inicializar());
    assertEquals(hash, usuarios.findByUsername("inst-admin").orElseThrow().getPasswordHash());
    assertFalse(usuarios.findByUsername("inst-almacen").orElseThrow().isActivo());
    assertEquals(5, eventos.count());
  }

  @Test
  void configuracionIncompletaNoCreaCuentasParciales() {
    config.getSupervisor().setPassword("");
    assertThrows(IllegalArgumentException.class, () -> instalacion.inicializar());
    assertEquals(0, usuarios.count());
    assertEquals(0, eventos.count());
    assertFalse(control.findById(1L).orElseThrow().isInstalacionCompleta());
  }

  @Test
  void nombresYClavesInicialesDuplicadasSonRechazados() {
    config.getCompras().setUsername(config.getAlmacen().getUsername());
    assertThrows(IllegalArgumentException.class, () -> instalacion.inicializar());
    assertEquals(0, usuarios.count());
    config.getCompras().setUsername("inst-compras");
    config.getCompras().setPassword(config.getAlmacen().getPassword());
    assertThrows(IllegalArgumentException.class, () -> instalacion.inicializar());
    assertEquals(0, usuarios.count());
  }

  @Test
  void dosInstanciasConcurrentesSoloInstalanUnaVez() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    try {
      var resultados =
          pool.invokeAll(
              List.<Callable<Boolean>>of(instalacion::inicializar, instalacion::inicializar));
      int instalaciones = 0;
      for (var f : resultados) if (f.get(20, TimeUnit.SECONDS)) instalaciones++;
      assertEquals(1, instalaciones);
      assertEquals(5, usuarios.count());
      assertEquals(5, eventos.count());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void errorDeAuditoriaRevierteTodaLaInstalacion() {
    doThrow(new org.springframework.dao.DataIntegrityViolationException("Fallo simulado"))
        .when(eventos)
        .save(any(EventoUsuario.class));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> instalacion.inicializar());
    assertEquals(0, usuarios.count());
    assertFalse(control.findById(1L).orElseThrow().isInstalacionCompleta());
  }

  @Test
  void unaBaseYaInstaladaNoRecreaUsuariosBorradosNiExigeOtraVezLasClavesIniciales() {
    instalacion.inicializar();
    Long id = usuarios.findByUsername("inst-compras").orElseThrow().getId();
    jdbc.update("delete from evento_usuario where usuario_id=?", id);
    jdbc.update("delete from usuario where id=?", id);
    config.getAlmacen().setPassword("");
    assertFalse(instalacion.inicializar());
    assertEquals(4, usuarios.count());
    assertTrue(usuarios.findByUsername("inst-compras").isEmpty());
  }
}
