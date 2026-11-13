package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.dto.ComprasDtos.*;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.service.ComprasService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ComprasController {
  private final ComprasService compras;

  public ComprasController(ComprasService c) {
    compras = c;
  }

  @PostMapping("/solicitudes-compra")
  public Solicitud crear(@Valid @RequestBody CrearSolicitud r) {
    return compras.crear(r);
  }

  @GetMapping("/solicitudes-compra/{id}")
  public Solicitud solicitud(@PathVariable Long id) {
    return compras.obtenerSolicitud(id);
  }

  @PostMapping("/solicitudes-compra/{id}/aprobar")
  public Solicitud aprobar(@PathVariable Long id, @Valid @RequestBody Aprobar r) {
    return compras.aprobar(id, r);
  }

  @PostMapping("/solicitudes-compra/{id}/rechazar")
  public Solicitud rechazar(@PathVariable Long id, @Valid @RequestBody Motivo r) {
    return compras.rechazar(id, r);
  }

  @PostMapping("/solicitudes-compra/{id}/confirmar-duplicidad")
  public Solicitud duplicada(@PathVariable Long id, @Valid @RequestBody Duplicidad r) {
    return compras.duplicada(id, r);
  }

  @PostMapping("/solicitudes-compra/{id}/emitir-orden")
  public Orden emitir(@PathVariable Long id) {
    return compras.emitir(id);
  }

  @GetMapping("/ordenes-compra/{id}")
  public Orden orden(@PathVariable Long id) {
    return compras.obtenerOrden(id);
  }

  @PostMapping("/ordenes-compra/{id}/recepciones")
  public Recepcion recibir(@PathVariable Long id, @Valid @RequestBody Recibir r) {
    return compras.recibir(id, r);
  }

  @PostMapping("/ordenes-compra/{id}/cancelar")
  public Orden cancelar(@PathVariable Long id, @Valid @RequestBody Motivo r) {
    return compras.cancelar(id, r);
  }

  @GetMapping("/ordenes-compra/{id}/recepciones")
  public Page<Recepcion> recepciones(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") @Min(0) int pagina,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
    return compras.recepciones(id, pagina, tamanio);
  }

  @GetMapping("/solicitudes-compra")
  public Page<Solicitud> solicitudes(
      @RequestParam(required = false) String sku,
      @RequestParam(required = false) Long almacenId,
      @RequestParam(required = false) EstadoSolicitudCompra estado,
      @RequestParam(defaultValue = "1970-01-01T00:00:00Z") Instant desde,
      @RequestParam(defaultValue = "9999-01-01T00:00:00Z") Instant hasta,
      @RequestParam(defaultValue = "0") @Min(0) int pagina,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
    return compras.listarSolicitudes(sku, almacenId, estado, desde, hasta, pagina, tamanio);
  }

  @GetMapping("/ordenes-compra")
  public Page<Orden> ordenes(
      @RequestParam(required = false) String sku,
      @RequestParam(required = false) Long almacenId,
      @RequestParam(required = false) EstadoOrdenCompra estado,
      @RequestParam(defaultValue = "1970-01-01T00:00:00Z") Instant desde,
      @RequestParam(defaultValue = "9999-01-01T00:00:00Z") Instant hasta,
      @RequestParam(defaultValue = "0") @Min(0) int pagina,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
    return compras.listarOrdenes(sku, almacenId, estado, desde, hasta, pagina, tamanio);
  }
}
