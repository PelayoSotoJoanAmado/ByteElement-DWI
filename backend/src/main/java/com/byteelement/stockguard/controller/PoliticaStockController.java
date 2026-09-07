package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.dto.GuardarPoliticaStockRequest;
import com.byteelement.stockguard.dto.PoliticaStockResponse;
import com.byteelement.stockguard.service.PoliticaStockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/productos/{productoId}/almacenes/{almacenId}/politica-stock")
public class PoliticaStockController {
    private final PoliticaStockService service;
    public PoliticaStockController(PoliticaStockService service) { this.service = service; }
    @GetMapping
    public PoliticaStockResponse obtener(@PathVariable @Positive Long productoId, @PathVariable @Positive Long almacenId) {
        return service.obtener(productoId, almacenId);
    }
    @PutMapping
    public PoliticaStockResponse guardar(@PathVariable @Positive Long productoId, @PathVariable @Positive Long almacenId,
                                        @Valid @RequestBody GuardarPoliticaStockRequest request) {
        return service.guardar(productoId, almacenId, request);
    }
}
