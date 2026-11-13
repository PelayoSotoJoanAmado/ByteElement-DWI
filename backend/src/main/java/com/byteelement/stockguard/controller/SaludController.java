package com.byteelement.stockguard.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class SaludController {
  private final JdbcTemplate jdbc;

  public SaludController(JdbcTemplate j) {
    jdbc = j;
  }

  @GetMapping("/api/v1/salud")
  public Map<String, Object> salud() {
    long inicio = System.nanoTime();
    jdbc.queryForObject("select 1", Integer.class);
    return Map.of(
        "estado",
        "UP",
        "baseDatos",
        "UP",
        "fecha",
        Instant.now(),
        "consultaMs",
        (System.nanoTime() - inicio) / 1_000_000.0);
  }
}
