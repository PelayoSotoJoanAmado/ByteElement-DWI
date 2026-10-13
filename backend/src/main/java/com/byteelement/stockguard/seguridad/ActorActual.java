package com.byteelement.stockguard.seguridad;

import com.byteelement.stockguard.entity.Usuario;
import com.byteelement.stockguard.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ActorActual {
  private final UsuarioRepository usuarios;

  public ActorActual(UsuarioRepository usuarios) {
    this.usuarios = usuarios;
  }

  public Usuario usuario() {
    return usuarios
        .findByUsername(nombre())
        .filter(Usuario::isActivo)
        .orElseThrow(
            () ->
                new AuthenticationCredentialsNotFoundException(
                    "Usuario autenticado no disponible"));
  }

  public String nombre() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getName()))
      throw new AuthenticationCredentialsNotFoundException("Sesión requerida");
    return a.getName();
  }
}
