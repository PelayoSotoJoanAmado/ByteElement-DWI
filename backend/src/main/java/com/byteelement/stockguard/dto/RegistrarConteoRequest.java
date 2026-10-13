package com.byteelement.stockguard.dto;

import jakarta.validation.constraints.*;

public record RegistrarConteoRequest(
    @NotNull @Positive Long productoId,
    @NotNull @Positive Long almacenId,
    @NotNull @PositiveOrZero Long cantidadFisica,
    @NotBlank @Size(max = 500) String motivo) {}
