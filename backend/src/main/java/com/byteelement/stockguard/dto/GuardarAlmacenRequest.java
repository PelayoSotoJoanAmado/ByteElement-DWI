package com.byteelement.stockguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardarAlmacenRequest(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 120) String nombre) {}
