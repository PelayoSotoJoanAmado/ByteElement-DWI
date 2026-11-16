package com.byteelement.stockguard.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.repository.UsuarioRepository;
import com.byteelement.stockguard.seguridad.Rol;
import com.byteelement.stockguard.service.UsuarioService;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ContratosSeguridadIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioService usuarios;
  @Autowired UsuarioRepository repositorio;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

  record Contrato(String metodo, String ruta, List<Rol> roles) {}

  @Test
  void RF09_matrizDeTodosLosContratosProtegidos() throws Exception {
    String password = "SecurityMatrixPassword123!";
    Map<Rol, String> tokens = new EnumMap<>(Rol.class);
    for (Rol rol : Rol.values()) {
      String nombre = "matriz-" + rol;
      if (repositorio.findByUsername(nombre).isEmpty())
        usuarios.crear(new CrearUsuarioRequest(nombre, password, rol));
      jdbc.update("update usuario set requiere_cambio_password=false where username=?", nombre);
      tokens.put(
          rol, (String) usuarios.login(new LoginRequest(nombre, password)).get("accessToken"));
    }
    List<Contrato> contratos = new ArrayList<>();
    List<Rol> todos = List.of(Rol.values());
    List<Rol> compras = List.of(Rol.ALMACEN, Rol.COMPRAS, Rol.SUPERVISOR, Rol.ADMINISTRADOR);
    agregar(
        contratos,
        "GET",
        todos,
        "/productos",
        "/productos/999999999",
        "/almacenes",
        "/almacenes/999999999",
        "/productos/999999999/almacenes/999999999/politica-stock",
        "/existencias",
        "/movimientos",
        "/movimientos/999999999",
        "/reportes/rotacion",
        "/reportes/stock-critico");
    agregar(
        contratos,
        "GET",
        compras,
        "/reposicion",
        "/solicitudes-compra",
        "/solicitudes-compra/999999999",
        "/ordenes-compra",
        "/ordenes-compra/999999999",
        "/ordenes-compra/999999999/recepciones");
    agregar(
        contratos, "GET", List.of(Rol.ALMACEN, Rol.SUPERVISOR), "/conteos", "/conteos/999999999");
    agregar(
        contratos,
        "GET",
        List.of(Rol.SUPERVISOR, Rol.ADMINISTRADOR),
        "/indicadores",
        "/dashboard",
        "/cortes-stock/inexistente",
        "/salud");
    agregar(contratos, "GET", List.of(Rol.ADMINISTRADOR), "/usuarios", "/evidencias");
    agregar(contratos, "GET", todos, "/auth/me");
    agregar(contratos, "POST", todos, "/auth/cambiar-password");
    agregar(contratos, "GET", List.of(Rol.ADMINISTRADOR), "/usuarios/999999999/eventos");
    agregar(contratos, "PUT", List.of(Rol.ADMINISTRADOR), "/usuarios/999999999");
    agregar(
        contratos, "POST", List.of(Rol.ADMINISTRADOR), "/usuarios/999999999/restablecer-password");
    agregar(contratos, "POST", List.of(Rol.ADMINISTRADOR), "/usuarios", "/productos", "/almacenes");
    agregar(
        contratos,
        "PUT",
        List.of(Rol.ADMINISTRADOR),
        "/productos/999999999",
        "/almacenes/999999999");
    agregar(
        contratos,
        "PATCH",
        List.of(Rol.ADMINISTRADOR),
        "/productos/999999999/desactivar",
        "/almacenes/999999999/desactivar");
    agregar(
        contratos,
        "PUT",
        List.of(Rol.SUPERVISOR),
        "/productos/999999999/almacenes/999999999/politica-stock");
    agregar(
        contratos,
        "POST",
        List.of(Rol.ALMACEN),
        "/movimientos",
        "/transferencias",
        "/conteos",
        "/ordenes-compra/999999999/recepciones");
    agregar(
        contratos,
        "POST",
        List.of(Rol.SUPERVISOR),
        "/conteos/999999999/confirmar",
        "/cortes-stock",
        "/solicitudes-compra/999999999/aprobar",
        "/solicitudes-compra/999999999/rechazar",
        "/solicitudes-compra/999999999/confirmar-duplicidad",
        "/ordenes-compra/999999999/cancelar");
    agregar(
        contratos,
        "POST",
        List.of(Rol.COMPRAS),
        "/solicitudes-compra",
        "/solicitudes-compra/999999999/emitir-orden");
    List<Map<String, Object>> resultados = new ArrayList<>();
    for (Contrato c : contratos) {
      assertEquals(401, ejecutar(c, null), c.toString());
      assertEquals(401, ejecutar(c, "invalido"), c.toString());
      Map<String, Integer> permitidos = new LinkedHashMap<>(), denegados = new LinkedHashMap<>();
      for (Rol rol : Rol.values()) {
        int status = ejecutar(c, tokens.get(rol));
        if (c.roles().contains(rol)) {
          // Se prueba autorizacion con cuerpos vacios/IDs inexistentes, no el exito del negocio.
          assertTrue(
              status >= 200 && status < 500 && status != 401 && status != 403,
              c + " rol=" + rol + " status=" + status);
          permitidos.put(rol.name(), status);
        } else {
          assertEquals(403, status, c + " rol=" + rol);
          denegados.put(rol.name(), status);
        }
      }
      resultados.add(
          Map.of(
              "metodo",
              c.metodo(),
              "ruta",
              c.ruta(),
              "tokenAusente",
              401,
              "tokenInvalido",
              401,
              "rolesPermitidos",
              permitidos,
              "rolesDenegados",
              denegados,
              "aprobado",
              true));
    }
    Path archivo = Path.of("target/contratos-seguridad.json");
    Files.writeString(
        archivo,
        tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(resultados));
  }

  private void agregar(List<Contrato> lista, String metodo, List<Rol> roles, String... rutas) {
    for (String ruta : rutas) lista.add(new Contrato(metodo, "/api/v1" + ruta, roles));
  }

  private int ejecutar(Contrato contrato, String token) throws Exception {
    var peticion = request(HttpMethod.valueOf(contrato.metodo()), contrato.ruta());
    if (token != null) peticion.header("Authorization", "Bearer " + token);
    if (!contrato.metodo().equals("GET")) peticion.contentType("application/json").content("{}");
    return mvc.perform(peticion).andReturn().getResponse().getStatus();
  }
}
