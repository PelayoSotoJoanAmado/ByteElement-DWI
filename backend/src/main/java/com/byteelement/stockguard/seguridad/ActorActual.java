package com.byteelement.stockguard.seguridad;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ActorActual {
  public String nombre() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getName()))
      throw new AuthenticationCredentialsNotFoundException("Sesión requerida");
    return a.getName();
  }
}
