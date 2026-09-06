package com.byteelement.stockguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearProductoRequest(

        @NotBlank @Size(max = 50)
        String sku,

        @NotBlank @Size(max = 80)
        String categoria,

        @NotBlank @Size(max = 80)
        String marca,

        @NotBlank @Size(max = 120)
        String modelo,

        @NotBlank @Size(max = 500)
        String especificacion,

        @NotNull
        Boolean critico
) {
}