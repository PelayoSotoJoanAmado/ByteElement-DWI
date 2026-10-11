package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.entity.Movimiento;
import com.byteelement.stockguard.service.InventarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class InventarioController {

  private final InventarioService inventarioService;

  public InventarioController(InventarioService inventarioService) {
    this.inventarioService = inventarioService;
  }

  @GetMapping("/almacenes")
  public List<AlmacenResponse> listarAlmacenes() {
    return inventarioService.almacenes();
  }

  @GetMapping("/almacenes/{id}")
  public AlmacenResponse obtenerAlmacen(@PathVariable Long id) {
    return inventarioService.obtenerAlmacen(id);
  }

  @PostMapping("/movimientos")
  public ResponseEntity<MovimientoResponse> registrarMovimiento(
      @Valid @RequestBody RegistrarMovimientoRequest request) {

    Movimiento movimiento = inventarioService.registrar(request);
    MovimientoResponse response = MovimientoResponse.desde(movimiento);

    return ResponseEntity.created(URI.create("/api/v1/movimientos/" + response.id()))
        .body(response);
  }

  @GetMapping("/movimientos")
  public org.springframework.data.domain.Page<MovimientoResponse> listarMovimientos(
      @RequestParam(required = false) Long productoId,
      @RequestParam(required = false) String sku,
      @RequestParam(required = false) Long almacenId,
      @RequestParam(defaultValue = "1970-01-01T00:00:00Z") java.time.Instant desde,
      @RequestParam(defaultValue = "9999-01-01T00:00:00Z") java.time.Instant hasta,
      @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int pagina,
      @RequestParam(defaultValue = "20")
          @jakarta.validation.constraints.Min(1)
          @jakarta.validation.constraints.Max(100)
          int tamanio) {
    return inventarioService.listarMovimientos(
        productoId, sku, almacenId, desde, hasta, pagina, tamanio);
  }

  @PostMapping("/conteos")
  public ResponseEntity<ConteoResponse> contar(@Valid @RequestBody RegistrarConteoRequest request) {
    var c = inventarioService.contar(request);
    return ResponseEntity.created(URI.create("/api/v1/conteos/" + c.id())).body(c);
  }

  @GetMapping("/conteos/{id}")
  public ConteoResponse obtenerConteo(@PathVariable Long id) {
    return inventarioService.obtenerConteo(id);
  }

  @PostMapping("/conteos/{id}/confirmar")
  public ConteoResponse confirmar(@PathVariable Long id) {
    return inventarioService.confirmarConteo(id);
  }

  @GetMapping("/movimientos/{id}")
  public MovimientoResponse obtenerMovimiento(@PathVariable Long id) {
    return MovimientoResponse.desde(inventarioService.obtenerMovimiento(id));
  }

  @PostMapping("/transferencias")
  public ResponseEntity<TransferenciaResponse> transferir(
      @Valid @RequestBody RegistrarTransferenciaRequest request) {

    TransferenciaResponse response = inventarioService.transferir(request);

    return ResponseEntity.created(
            URI.create("/api/v1/movimientos/" + response.movimientoSalidaId()))
        .body(response);
  }

  @GetMapping("/existencias")
  public ExistenciaResponse consultarExistencia(
      @RequestParam @NotNull @Positive Long productoId,
      @RequestParam @NotNull @Positive Long almacenId) {
    return inventarioService.consultar(productoId, almacenId);
  }
}
