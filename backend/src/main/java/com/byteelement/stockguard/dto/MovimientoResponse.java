package com.byteelement.stockguard.dto;

import com.byteelement.stockguard.entity.Movimiento;
import com.byteelement.stockguard.entity.TipoMovimiento;
import java.time.Instant;

public record MovimientoResponse(long id, long productoId, long almacenId,
                                 TipoMovimiento tipo, long cantidad, String autor,
                                 Instant fecha, String motivo, long saldoResultante) {
    public static MovimientoResponse desde(Movimiento movimiento) {
        return new MovimientoResponse(movimiento.id(), movimiento.productoId(),
                movimiento.almacenId(), movimiento.tipo(), movimiento.cantidad(),
                movimiento.autor(), movimiento.fecha(), movimiento.motivo(),
                movimiento.saldoResultante());
    }
}
