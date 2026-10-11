package com.byteelement.stockguard.config;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.repository.*;
import com.byteelement.stockguard.seguridad.Rol;
import com.byteelement.stockguard.service.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Datos optativos de exposición: no se cargan fuera del perfil demo. */
@Component
@Profile("demo")
@Order(1)
public class DatosDemo implements ApplicationRunner {
  private final UsuarioRepository users;
  private final UsuarioService usuarios;
  private final ProductoRepository productos;
  private final AlmacenRepository almacenes;
  private final ProductoService catalogo;
  private final PoliticaStockService politicas;
  private final InventarioService inventario;
  private final ReporteService reportes;
  private final String password;

  public DatosDemo(
      UsuarioRepository users,
      UsuarioService usuarios,
      ProductoRepository productos,
      AlmacenRepository almacenes,
      ProductoService catalogo,
      PoliticaStockService politicas,
      InventarioService inventario,
      ReporteService reportes,
      @Value("${stockguard.demo.password}") String password) {
    this.users = users;
    this.usuarios = usuarios;
    this.productos = productos;
    this.almacenes = almacenes;
    this.catalogo = catalogo;
    this.politicas = politicas;
    this.inventario = inventario;
    this.reportes = reportes;
    this.password = password;
  }

  @org.springframework.transaction.annotation.Transactional
  public void run(ApplicationArguments args) {
    if (password.length() < 12
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "Perfil demo requiere DEMO_PASSWORD de 12 caracteres y máximo 72 bytes");
    for (Rol rol : Rol.values())
      if (users.findByUsername("demo-" + rol.name().toLowerCase()).isEmpty())
        usuarios.crear(new CrearUsuarioRequest("demo-" + rol.name().toLowerCase(), password, rol));
    // El marcador es el primer SKU: un reinicio no vuelve a sumar las entradas.
    if (productos.existsBySku("DEMO-RAM")) return;
    if (productos.existsBySku("DEMO-SSD"))
      throw new IllegalStateException(
          "Catálogo demo incompleto: utilice una base de exposición vacía");
    var a =
        almacenes.findAll().stream()
            .filter(x -> x.isActivo() && x.getCodigo().equals("A01"))
            .findFirst()
            .orElseThrow();
    var b =
        almacenes.findAll().stream()
            .filter(x -> x.isActivo() && x.getCodigo().equals("A02"))
            .findFirst()
            .orElseThrow();
    var p =
        catalogo.crear(new CrearProductoRequest("DEMO-RAM", "RAM", "Demo", "DDR4", "8 GB", true));
    var q =
        catalogo.crear(new CrearProductoRequest("DEMO-SSD", "SSD", "Demo", "NVMe", "500 GB", true));
    try {
      login("demo-supervisor", Rol.SUPERVISOR);
      for (var producto : List.of(p, q))
        for (var almacen : almacenes.findAll())
          if (almacen.isActivo())
            politicas.guardar(
                producto.getId(), almacen.getId(), new GuardarPoliticaStockRequest(2L, 10L));
      login("demo-almacen", Rol.ALMACEN);
      inventario.registrar(
          new RegistrarMovimientoRequest(
              p.getId(), a.getId(), TipoMovimiento.ENTRADA, 15L, null, "Saldo inicial demo"));
      inventario.registrar(
          new RegistrarMovimientoRequest(
              q.getId(), a.getId(), TipoMovimiento.ENTRADA, 8L, null, "Saldo inicial demo"));
      inventario.transferir(
          new RegistrarTransferenciaRequest(
              q.getId(), a.getId(), b.getId(), 3L, null, "Transferencia demo"));
      inventario.contar(
          new RegistrarConteoRequest(p.getId(), a.getId(), 15L, "Conteo coincidente"));
      inventario.contar(
          new RegistrarConteoRequest(q.getId(), a.getId(), 4L, "Conteo con diferencia"));
      login("demo-supervisor", Rol.SUPERVISOR);
      reportes.crearCorte();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void login(String nombre, Rol rol) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                nombre, "", List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
  }
}
