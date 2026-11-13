package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.dto.ComprasDtos.Reposicion;
import com.byteelement.stockguard.service.ReposicionService;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reposicion")
public class ReposicionController {
  private final ReposicionService servicio;

  public ReposicionController(ReposicionService s) {
    servicio = s;
  }

  @GetMapping
  public Page<Reposicion> listar(
      @RequestParam(required = false) String sku,
      @RequestParam(required = false) Long almacenId,
      @RequestParam(defaultValue = "0") @Min(0) int pagina,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
    return servicio.listar(sku, almacenId, pagina, tamanio);
  }
}
