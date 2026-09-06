package com.byteelement.stockguard.dto;

import com.byteelement.stockguard.entity.Producto;

public record ProductoResponse(
        Long id,
        String sku,
        String categoria,
        String marca,
        String modelo,
        String especificacion,
        boolean critico,
        boolean activo
) {
    public static ProductoResponse desde(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getSku(),
                producto.getCategoria(),
                producto.getMarca(),
                producto.getModelo(),
                producto.getEspecificacion(),
                producto.isCritico(),
                producto.isActivo()
        );
    }
}