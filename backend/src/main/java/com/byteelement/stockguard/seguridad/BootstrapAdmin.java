package com.byteelement.stockguard.seguridad;

import com.byteelement.stockguard.dto.CrearUsuarioRequest;
import com.byteelement.stockguard.repository.UsuarioRepository;
import com.byteelement.stockguard.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class BootstrapAdmin implements ApplicationRunner {
  private final UsuarioRepository users;
  private final UsuarioService service;
  private final String username, password;

  public BootstrapAdmin(
      UsuarioRepository users,
      UsuarioService service,
      @Value("${stockguard.bootstrap.username}") String username,
      @Value("${stockguard.bootstrap.password}") String password) {
    this.users = users;
    this.service = service;
    this.username = username;
    this.password = password;
  }

  public void run(ApplicationArguments args) {
    if (users.count() != 0) return;
    if (!username.matches("[a-zA-Z0-9._-]{3,80}")
        || password.length() < 12
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "Base sin usuarios: configure BOOTSTRAP_USER y BOOTSTRAP_PASSWORD (mínimo 12 caracteres,"
              + " máximo 72 bytes)");
    service.crear(new CrearUsuarioRequest(username, password, Rol.ADMINISTRADOR));
  }
}
