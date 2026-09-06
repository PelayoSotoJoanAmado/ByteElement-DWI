package com.byteelement.stockguard.dto;

import com.byteelement.stockguard.entity.TipoMovimiento;
import jakarta.validation.constraints.*;

public record RegistrarMovimientoRequest(
        @NotNull @Positive Long productoId,
        @NotNull @Positive Long almacenId,
        @NotNull TipoMovimiento tipo,
        @NotNull @Positive Long cantidad,
        @NotBlank @Size(max = 100) String autor,
        @NotBlank @Size(max = 500) String motivo) {
}
