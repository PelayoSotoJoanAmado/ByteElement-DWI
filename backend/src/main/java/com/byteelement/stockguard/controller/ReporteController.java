package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.service.ReporteService;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ReporteController {
  private final ReporteService reportes;

  public ReporteController(ReporteService reportes) {
    this.reportes = reportes;
  }

  @PostMapping("/cortes-stock")
  public List<CorteStockResponse> corte() {
    return reportes.crearCorte();
  }

  @GetMapping("/cortes-stock/{lote}")
  public List<CorteStockResponse> corte(@PathVariable String lote) {
    return reportes.obtenerCorte(lote);
  }

  @GetMapping("/indicadores")
  public IndicadorResponse indicador(
      @RequestParam String codigo,
      @RequestParam Instant desde,
      @RequestParam Instant hasta,
      @RequestParam(required = false) String sku,
      @RequestParam(required = false) Long almacenId) {
    return reportes.indicador(codigo, desde, hasta, sku, almacenId);
  }

  @GetMapping("/reportes/{tipo}")
  public Page<Map<String, Object>> reporte(
      @PathVariable String tipo,
      @RequestParam Instant desde,
      @RequestParam Instant hasta,
      @RequestParam(required = false) String sku,
      @RequestParam(required = false) Long almacenId,
      @RequestParam(defaultValue = "0") @Min(0) int pagina,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
    if (tipo.equals("rotacion"))
      return reportes.rotacion(sku, almacenId, desde, hasta, pagina, tamanio);
    if (tipo.equals("stock-critico"))
      return reportes.stockCritico(sku, almacenId, desde, hasta, pagina, tamanio);
    throw new IllegalArgumentException("Reporte disponible: rotacion o stock-critico");
  }
}
