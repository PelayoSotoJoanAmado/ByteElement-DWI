package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.dto.AlmacenResponse;
import com.byteelement.stockguard.dto.GuardarAlmacenRequest;
import com.byteelement.stockguard.service.AlmacenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/almacenes")
public class AlmacenController {
    private final AlmacenService service;
    public AlmacenController(AlmacenService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<AlmacenResponse> crear(@Valid @RequestBody GuardarAlmacenRequest request) {
        var response = AlmacenResponse.desde(service.crear(request));
        return ResponseEntity.created(URI.create("/api/v1/almacenes/" + response.id())).body(response);
    }
    @PutMapping("/{id}")
    public AlmacenResponse actualizar(@PathVariable Long id, @Valid @RequestBody GuardarAlmacenRequest request) {
        return AlmacenResponse.desde(service.actualizar(id, request));
    }
    @PatchMapping("/{id}/desactivar")
    public AlmacenResponse desactivar(@PathVariable Long id) { return AlmacenResponse.desde(service.desactivar(id)); }
}
