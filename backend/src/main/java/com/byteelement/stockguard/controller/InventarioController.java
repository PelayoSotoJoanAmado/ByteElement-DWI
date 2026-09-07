package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.entity.Movimiento;
import com.byteelement.stockguard.service.InventarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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

        return ResponseEntity
                .created(URI.create("/api/v1/movimientos/" + response.id()))
                .body(response);
    }

    @GetMapping("/movimientos")
    public List<MovimientoResponse> listarMovimientos(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long almacenId) {
        return inventarioService.listarMovimientos(productoId, almacenId)
                .stream()
                .map(MovimientoResponse::desde)
                .toList();
    }

    @GetMapping("/movimientos/{id}")
    public MovimientoResponse obtenerMovimiento(@PathVariable Long id) {
        return MovimientoResponse.desde(inventarioService.obtenerMovimiento(id));
    }

    @PostMapping("/transferencias")
    public ResponseEntity<TransferenciaResponse> transferir(
            @Valid @RequestBody RegistrarTransferenciaRequest request) {

        TransferenciaResponse response = inventarioService.transferir(request);

        return ResponseEntity
                .created(URI.create("/api/v1/movimientos/" + response.movimientoSalidaId()))
                .body(response);
    }

    @GetMapping("/existencias")
    public ExistenciaResponse consultarExistencia(
            @RequestParam @NotNull @Positive Long productoId,
            @RequestParam @NotNull @Positive Long almacenId) {
        return inventarioService.consultar(productoId, almacenId);
    }
}
