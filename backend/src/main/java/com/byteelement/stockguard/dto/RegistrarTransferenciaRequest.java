package com.byteelement.stockguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegistrarTransferenciaRequest(
        @NotNull @Positive Long productoId,
        @NotNull @Positive Long origenAlmacenId,
        @NotNull @Positive Long destinoAlmacenId,
        @NotNull @Positive Long cantidad,
        @NotBlank @Size(max = 100) String autor,
        @NotBlank @Size(max = 500) String motivo
) {
}

