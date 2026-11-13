package com.byteelement.stockguard.controller;

import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/v1/evidencias")
public class EvidenciaController {
  private final Path archivo;

  public EvidenciaController(
      @Value("${stockguard.evidencias.archivo:./docs/APF3/evidencias/resultado.json}")
          String ruta) {
    archivo = Path.of(ruta);
  }

  @GetMapping
  public Map<String, Object> consultar(@RequestParam(defaultValue = "todos") String tipo)
      throws java.io.IOException {
    if (!Set.of("todos", "requisitos", "seguridad", "e2e", "reglas").contains(tipo))
      throw new IllegalArgumentException("Tipo de evidencia invalido");
    if (!Files.isRegularFile(archivo))
      return Map.of(
          "sinDatos",
          true,
          "tipo",
          tipo,
          "mensaje",
          "Genere evidencias con Verificar-APF3.ps1. No se presume cumplimiento sin ejecutar"
              + " pruebas.");
    var datos = JsonMapper.builder().build().readValue(Files.readAllBytes(archivo), Map.class);
    if (tipo.equals("todos")) return datos;
    return Map.of(
        "sinDatos",
        false,
        "version",
        datos.get("version"),
        "generadoEn",
        datos.get("generadoEn"),
        "resultado",
        datos.get(tipo));
  }
}
