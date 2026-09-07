package com.byteelement.stockguard.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record GuardarPoliticaStockRequest(
        @NotNull @PositiveOrZero Long minimo,
        @NotNull @PositiveOrZero Long maximo) {}
