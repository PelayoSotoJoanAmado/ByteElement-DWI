package com.byteelement.stockguard.seguridad;

import com.byteelement.stockguard.repository.UsuarioRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** Restriccion real del servidor: un guard Angular no basta para el primer acceso. */
public class CambioPasswordObligatorioFilter extends OncePerRequestFilter {
  private final UsuarioRepository usuarios;

  public CambioPasswordObligatorioFilter(UsuarioRepository u) {
    usuarios = u;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
      var u = usuarios.findByUsername(jwt.getName()).orElseThrow();
      boolean propia =
          req.getMethod().equals("GET") && req.getRequestURI().equals("/api/v1/auth/me")
              || req.getMethod().equals("POST")
                  && req.getRequestURI().equals("/api/v1/auth/cambiar-password");
      if (u.isRequiereCambioPassword() && !propia) {
        res.setStatus(403);
        res.setCharacterEncoding("UTF-8");
        res.setContentType("application/problem+json");
        res.setHeader("Cache-Control", "no-store");
        res.getWriter()
            .write(
                "{\"type\":\"about:blank\",\"title\":\"Cambio de password"
                    + " requerido\",\"status\":403,\"codigo\":\"CAMBIO_PASSWORD_REQUERIDO\",\"detail\":\"Debe"
                    + " cambiar su password inicial antes de operar\"}");
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
