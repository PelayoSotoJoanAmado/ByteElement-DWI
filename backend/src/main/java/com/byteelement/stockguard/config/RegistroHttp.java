package com.byteelement.stockguard.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RegistroHttp extends OncePerRequestFilter {
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RegistroHttp.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long inicio = System.nanoTime();
    try {
      chain.doFilter(request, response);
    } finally {
      LOG.info(
          "http metodo={} ruta={} estado={} duracionMs={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          (System.nanoTime() - inicio) / 1_000_000.0);
    }
  }
}
