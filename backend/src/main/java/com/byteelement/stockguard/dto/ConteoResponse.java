package com.byteelement.stockguard.dto;

import com.byteelement.stockguard.entity.Conteo;
import java.time.Instant;

public record ConteoResponse(
    Long id,
    Long productoId,
    Long almacenId,
    long cantidadFisica,
    long saldoCorte,
    Instant fechaCorte,
    String autor,
    String motivo,
    boolean confirmado,
    String confirmadoPor,
    Instant confirmadoEn,
    Long movimientoId) {
  public static ConteoResponse desde(Conteo c) {
    return new ConteoResponse(
        c.getId(),
        c.getProducto().getId(),
        c.getAlmacen().getId(),
        c.getCantidadFisica(),
        c.getSaldoCorte(),
        c.getFechaCorte(),
        c.getAutor(),
        c.getMotivo(),
        c.isConfirmado(),
        c.getConfirmadoPor(),
        c.getConfirmadoEn(),
        c.getMovimientoId());
  }
}
