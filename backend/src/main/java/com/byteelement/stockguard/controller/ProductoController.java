package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.dto.CrearProductoRequest;
import com.byteelement.stockguard.dto.ProductoResponse;
import com.byteelement.stockguard.service.ProductoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(
            @Valid @RequestBody CrearProductoRequest request) {

        ProductoResponse respuesta =
                ProductoResponse.desde(service.crear(request));

        return ResponseEntity
                .created(URI.create("/api/v1/productos/" + respuesta.id()))
                .body(respuesta);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody CrearProductoRequest request) {
        return ProductoResponse.desde(service.actualizar(id, request));
    }

    @PatchMapping("/{id}/desactivar")
    public ProductoResponse desactivar(@PathVariable Long id) {
        return ProductoResponse.desde(service.desactivar(id));
    }

    @GetMapping
    public List<ProductoResponse> listar(
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {

        return service.listar(pagina, tamanio)
                .map(ProductoResponse::desde)
                .getContent();
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return ProductoResponse.desde(service.obtener(id));
    }
}
