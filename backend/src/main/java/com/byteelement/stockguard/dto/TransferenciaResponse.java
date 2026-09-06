package com.byteelement.stockguard.dto;

import java.time.Instant;

public record TransferenciaResponse(
        long movimientoSalidaId,
        long movimientoEntradaId,
        long productoId,
        long origenAlmacenId,
        long destinoAlmacenId,
        long cantidad,
        long saldoOrigenResultante,
        long saldoDestinoResultante,
        String autor,
        String motivo,
        Instant fecha
) {
}

