package com.byteelement.stockguard.dto;

import com.byteelement.stockguard.entity.PoliticaStock;

public record PoliticaStockResponse(Long id, Long productoId, Long almacenId, long minimo, long maximo) {
    public static PoliticaStockResponse desde(PoliticaStock politica) {
        return new PoliticaStockResponse(politica.getId(), politica.getProducto().getId(),
                politica.getAlmacen().getId(), politica.getMinimo(), politica.getMaximo());
    }
}
