package com.byteelement.stockguard.entity;

import java.time.Instant;

public record Movimiento(long id, long productoId, long almacenId,
                         TipoMovimiento tipo, long cantidad, String autor,
                         Instant fecha, String motivo, long saldoResultante) {
}
