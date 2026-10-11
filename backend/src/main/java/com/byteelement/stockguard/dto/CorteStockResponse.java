package com.byteelement.stockguard.dto;

import com.byteelement.stockguard.entity.CorteStock;
import java.time.Instant;

public record CorteStockResponse(
    Long id,
    String lote,
    Instant fecha,
    Long productoId,
    Long almacenId,
    String sku,
    boolean critico,
    long cantidad,
    Long minimo,
    Long maximo) {
  public static CorteStockResponse desde(CorteStock c) {
    return new CorteStockResponse(
        c.getId(),
        c.getLote(),
        c.getFecha(),
        c.getProductoId(),
        c.getAlmacenId(),
        c.getSku(),
        c.isCritico(),
        c.getCantidad(),
        c.getMinimo(),
        c.getMaximo());
  }
}
