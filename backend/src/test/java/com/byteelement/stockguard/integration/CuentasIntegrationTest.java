package com.byteelement.stockguard.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.seguridad.Rol;
import com.byteelement.stockguard.service.UsuarioService;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CuentasIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioService usuarios;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired org.springframework.security.oauth2.jwt.JwtEncoder encoder;
  static final String INICIAL = "InicialDePrueba123!", DEFINITIVA = "DefinitivaDePrueba456!";

  record Cuenta(long id, String nombre, String token, long version) {}

  Cuenta cuenta(Rol rol, boolean completar) throws Exception {
    String nombre = "u-" + UUID.randomUUID().toString().substring(0, 12);
    var u = usuarios.crear(new CrearUsuarioRequest(nombre, INICIAL, rol));
    String token = (String) usuarios.login(new LoginRequest(nombre, INICIAL)).get("accessToken");
    if (!completar) return new Cuenta(u.id(), nombre, token, u.version());
    var response =
        mvc.perform(
                post("/api/v1/auth/cambiar-password")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "passwordActual",
                                INICIAL,
                                "passwordNueva",
                                DEFINITIVA,
                                "confirmacionPassword",
                                DEFINITIVA))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var node = json.readTree(response);
    return new Cuenta(
        u.id(),
        nombre,
        node.get("accessToken").asString(),
        node.get("usuario").get("version").asLong());
  }

  Map<String, Object> editar(Cuenta c, String nombre, Rol rol, boolean activo) {
    return Map.of(
        "username",
        nombre,
        "rol",
        rol,
        "activo",
        activo,
        "version",
        c.version(),
        "motivo",
        "Cambio autorizado de prueba");
  }

  String body(Object v) {
    return json.writeValueAsString(v);
  }

  @Test
  void tokenAnteriorAV8ConFirmaYRolValidosNoPermiteAccesoSinIdentidadYVersion() throws Exception {
    var u = cuenta(Rol.ALMACEN, true);
    var ahora = java.time.Instant.now();
    var claims =
        org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
            .issuer("stockguard")
            .subject(u.nombre())
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(300))
            .claim("rol", Rol.ALMACEN.name())
            .build();
    var token =
        encoder
            .encode(
                org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                    org.springframework.security.oauth2.jwt.JwsHeader.with(
                            org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                        .build(),
                    claims))
            .getTokenValue();
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk());
  }

  @Test
  void reutilizarNombreNoPermiteQueUnTokenAnteriorSuplanteOtraCuenta() throws Exception {
    var admin = cuenta(Rol.ADMINISTRADOR, true);
    var anterior = cuenta(Rol.ALMACEN, true);
    mvc.perform(
            put("/api/v1/usuarios/" + anterior.id())
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(editar(anterior, anterior.nombre() + "-renombrado", Rol.ALMACEN, true))))
        .andExpect(status().isOk());
    var alta =
        mvc.perform(
                post("/api/v1/usuarios")
                    .header("Authorization", "Bearer " + admin.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        body(new CrearUsuarioRequest(anterior.nombre(), INICIAL, Rol.ALMACEN))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    long nuevoId = json.readTree(alta.getContentAsString()).get("id").asLong();
    assertNotEquals(anterior.id(), nuevoId);
    String inicial =
        (String) usuarios.login(new LoginRequest(anterior.nombre(), INICIAL)).get("accessToken");
    var cambio =
        mvc.perform(
                post("/api/v1/auth/cambiar-password")
                    .header("Authorization", "Bearer " + inicial)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        body(
                            Map.of(
                                "passwordActual", INICIAL,
                                "passwordNueva", DEFINITIVA,
                                "confirmacionPassword", DEFINITIVA))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    String nuevoToken = json.readTree(cambio.getContentAsString()).get("accessToken").asString();
    // Mismo nombre, rol y version de sesion que el token original: el uid debe distinguirlos.
    assertEquals(
        1L,
        jdbc.queryForObject("select version_sesion from usuario where id=?", Long.class, nuevoId));
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + anterior.token()))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + nuevoToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(nuevoId));
  }

  @Test
  void primerAccesoBloqueaTodosLosModulosPeroPermiteCambiarPassword() throws Exception {
    for (Rol rol : Rol.values()) {
      var c = cuenta(rol, false);
      for (String ruta :
          List.of(
              "/api/v1/productos", "/api/v1/usuarios", "/api/v1/reposicion", "/api/v1/dashboard")) {
        mvc.perform(get(ruta).header("Authorization", "Bearer " + c.token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("CAMBIO_PASSWORD_REQUERIDO"));
      }
      mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + c.token()))
          .andExpect(status().isOk());
    }
  }

  @Test
  void cambioPasswordValidaActualConfirmacionReutilizacionYBytesSinModificarCuenta()
      throws Exception {
    var c = cuenta(Rol.ALMACEN, false);
    var casos =
        List.of(
            Map.of(
                "passwordActual",
                "Incorrecta",
                "passwordNueva",
                DEFINITIVA,
                "confirmacionPassword",
                DEFINITIVA),
            Map.of(
                "passwordActual",
                INICIAL,
                "passwordNueva",
                DEFINITIVA,
                "confirmacionPassword",
                "NoCoincide123!"),
            Map.of(
                "passwordActual",
                INICIAL,
                "passwordNueva",
                INICIAL,
                "confirmacionPassword",
                INICIAL),
            Map.of(
                "passwordActual",
                INICIAL,
                "passwordNueva",
                "corta",
                "confirmacionPassword",
                "corta"),
            Map.of(
                "passwordActual",
                INICIAL,
                "passwordNueva",
                "á".repeat(37),
                "confirmacionPassword",
                "á".repeat(37)));
    for (var r : casos)
      mvc.perform(
              post("/api/v1/auth/cambiar-password")
                  .header("Authorization", "Bearer " + c.token())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body(r)))
          .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + c.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiereCambioPassword").value(true));
  }

  @Test
  void cambioPasswordInvalidaTodasLasSesionesYLaClaveAnterior() throws Exception {
    var c = cuenta(Rol.COMPRAS, false);
    String segundo =
        (String) usuarios.login(new LoginRequest(c.nombre(), INICIAL)).get("accessToken");
    var response =
        mvc.perform(
                post("/api/v1/auth/cambiar-password")
                    .header("Authorization", "Bearer " + c.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        body(
                            Map.of(
                                "passwordActual",
                                INICIAL,
                                "passwordNueva",
                                DEFINITIVA,
                                "confirmacionPassword",
                                DEFINITIVA))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String nuevo = json.readTree(response).get("accessToken").asString();
    for (String anterior : List.of(c.token(), segundo))
      mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + anterior))
          .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/reposicion").header("Authorization", "Bearer " + nuevo))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", c.nombre(), "password", INICIAL))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void adminCreaCuentaConCambioObligatorioSinDevolverSecretos() throws Exception {
    var admin = cuenta(Rol.ADMINISTRADOR, true);
    for (Rol rol : Rol.values()) {
      String nombre = "alta-" + UUID.randomUUID().toString().substring(0, 8);
      mvc.perform(
              post("/api/v1/usuarios")
                  .header("Authorization", "Bearer " + admin.token())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body(Map.of("username", nombre, "password", INICIAL, "rol", rol))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.requiereCambioPassword").value(true))
          .andExpect(jsonPath("$.rol").value(rol.name()))
          .andExpect(jsonPath("$.password").doesNotExist())
          .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
  }

  @Test
  void soloAdminConClaveDefinitivaPuedeGestionarCuentas() throws Exception {
    for (Rol rol : List.of(Rol.ALMACEN, Rol.COMPRAS, Rol.SUPERVISOR, Rol.CONSULTA_COMERCIAL)) {
      var c = cuenta(rol, true);
      mvc.perform(
              post("/api/v1/usuarios")
                  .header("Authorization", "Bearer " + c.token())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isForbidden());
      mvc.perform(
              put("/api/v1/usuarios/1")
                  .header("Authorization", "Bearer " + c.token())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isForbidden());
      mvc.perform(
              post("/api/v1/usuarios/1/restablecer-password")
                  .header("Authorization", "Bearer " + c.token())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isForbidden());
      mvc.perform(get("/api/v1/usuarios/1/eventos").header("Authorization", "Bearer " + c.token()))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void adminEditaNombreRolYEstadoRevocandoTokens() throws Exception {
    var admin = cuenta(Rol.ADMINISTRADOR, true);
    var u = cuenta(Rol.ALMACEN, true);
    String nombre = "renombrado-" + UUID.randomUUID().toString().substring(0, 8);
    mvc.perform(
            put("/api/v1/usuarios/" + u.id())
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(editar(u, nombre, Rol.COMPRAS, true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(nombre))
        .andExpect(jsonPath("$.rol").value("COMPRAS"));
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isUnauthorized());
    assertThrows(
        org.springframework.security.authentication.BadCredentialsException.class,
        () -> usuarios.login(new LoginRequest(u.nombre(), DEFINITIVA)));
    String nuevo = (String) usuarios.login(new LoginRequest(nombre, DEFINITIVA)).get("accessToken");
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + nuevo))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(u.id()))
        .andExpect(jsonPath("$.requiereCambioPassword").value(false));
  }

  @Test
  void desactivacionBloqueaLoginYReactivacionNoReviveTokenViejo() throws Exception {
    var admin = cuenta(Rol.ADMINISTRADOR, true);
    var u = cuenta(Rol.SUPERVISOR, true);
    var result =
        mvc.perform(
                put("/api/v1/usuarios/" + u.id())
                    .header("Authorization", "Bearer " + admin.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(editar(u, u.nombre(), Rol.SUPERVISOR, false))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isUnauthorized());
    assertThrows(
        org.springframework.security.authentication.BadCredentialsException.class,
        () -> usuarios.login(new LoginRequest(u.nombre(), DEFINITIVA)));
    var version = json.readTree(result).get("version").asLong();
    var desactivado = new Cuenta(u.id(), u.nombre(), u.token(), version);
    mvc.perform(
            put("/api/v1/usuarios/" + u.id())
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(editar(desactivado, u.nombre(), Rol.SUPERVISOR, true))))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isUnauthorized());
    assertNotNull(usuarios.login(new LoginRequest(u.nombre(), DEFINITIVA)).get("accessToken"));
  }

  @Test
  void resetObligaCambioRevocaSesionYConservaHistorialSinPassword() throws Exception {
    var admin = cuenta(Rol.ADMINISTRADOR, true);
    var u = cuenta(Rol.COMPRAS, true);
    String temporal = "TemporalSeguro789!";
    mvc.perform(
            post("/api/v1/usuarios/" + u.id() + "/restablecer-password")
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        Map.of(
                            "passwordTemporal",
                            temporal,
                            "confirmacionPassword",
                            temporal,
                            "motivo",
                            "Recuperacion presencial",
                            "version",
                            u.version()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiereCambioPassword").value(true))
        .andExpect(jsonPath("$.passwordTemporal").doesNotExist());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isUnauthorized());
    assertThrows(
        org.springframework.security.authentication.BadCredentialsException.class,
        () -> usuarios.login(new LoginRequest(u.nombre(), DEFINITIVA)));
    String token =
        (String) usuarios.login(new LoginRequest(u.nombre(), temporal)).get("accessToken");
    mvc.perform(get("/api/v1/reposicion").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("CAMBIO_PASSWORD_REQUERIDO"));
    String historia =
        mvc.perform(
                get("/api/v1/usuarios/" + u.id() + "/eventos")
                    .header("Authorization", "Bearer " + admin.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(historia.contains("RESTABLECIMIENTO_PASSWORD"));
    assertFalse(historia.contains(temporal));
    assertFalse(historia.contains("passwordHash"));
    assertFalse(historia.contains(INICIAL));
  }

  @Test
  void versionObsoletaYNombreDuplicadoNoDejanCambios() throws Exception {
    var admin = cuenta(Rol.ADMINISTRADOR, true);
    var u = cuenta(Rol.ALMACEN, true);
    var otro = cuenta(Rol.COMPRAS, true);
    mvc.perform(
            put("/api/v1/usuarios/" + u.id())
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(editar(u, otro.nombre(), Rol.ALMACEN, true))))
        .andExpect(status().isConflict());
    var viejo = new Cuenta(u.id(), u.nombre(), u.token(), u.version() + 3);
    mvc.perform(
            put("/api/v1/usuarios/" + u.id())
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(editar(viejo, u.nombre(), Rol.ALMACEN, false))))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk());
  }

  @Test
  void adminNoPuedeDesactivarseCambiarSuRolNiRestablecersePorAdministracion() throws Exception {
    var admin = cuenta(Rol.ADMINISTRADOR, true);
    mvc.perform(
            put("/api/v1/usuarios/" + admin.id())
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(editar(admin, admin.nombre(), Rol.ALMACEN, false))))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/v1/usuarios/" + admin.id() + "/restablecer-password")
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        Map.of(
                            "passwordTemporal",
                            "TemporalSeguro789!",
                            "confirmacionPassword",
                            "TemporalSeguro789!",
                            "motivo",
                            "Prueba",
                            "version",
                            admin.version()))))
        .andExpect(status().isConflict());
  }

  @Test
  void cambiosConcurrentesRespetanLaVersionDelFormulario() throws Exception {
    var admin = cuenta(Rol.ADMINISTRADOR, true);
    var u = cuenta(Rol.ALMACEN, true);
    var pool = Executors.newFixedThreadPool(2);
    try {
      List<Callable<Integer>> tareas =
          List.of(
              () ->
                  mvc.perform(
                          put("/api/v1/usuarios/" + u.id())
                              .header("Authorization", "Bearer " + admin.token())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(body(editar(u, u.nombre(), Rol.COMPRAS, true))))
                      .andReturn()
                      .getResponse()
                      .getStatus(),
              () ->
                  mvc.perform(
                          put("/api/v1/usuarios/" + u.id())
                              .header("Authorization", "Bearer " + admin.token())
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(body(editar(u, u.nombre(), Rol.SUPERVISOR, true))))
                      .andReturn()
                      .getResponse()
                      .getStatus());
      var estados = new ArrayList<Integer>();
      for (var f : pool.invokeAll(tareas)) estados.add(f.get(20, TimeUnit.SECONDS));
      Collections.sort(estados);
      assertEquals(List.of(200, 409), estados);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void unaCuentaPuedeConsultarSuEstadoYCambiarSuPasswordInicial() throws Exception {
    String nombre = "cuenta-" + UUID.randomUUID().toString().substring(0, 8);
    usuarios.crear(new CrearUsuarioRequest(nombre, "InicialDePrueba123!", Rol.COMPRAS));
    String token =
        (String) usuarios.login(new LoginRequest(nombre, "InicialDePrueba123!")).get("accessToken");
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(nombre))
        .andExpect(jsonPath("$.requiereCambioPassword").value(true));
    mvc.perform(
            post("/api/v1/auth/cambiar-password")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(
                    "{\"passwordActual\":\"InicialDePrueba123!\",\"passwordNueva\":\"NuevaDePrueba456!\",\"confirmacionPassword\":\"NuevaDePrueba456!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usuario.requiereCambioPassword").value(false));
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }
}
