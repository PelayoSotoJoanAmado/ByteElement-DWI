package com.byteelement.stockguard.dto;

import com.byteelement.stockguard.seguridad.Rol;
import jakarta.validation.constraints.*;

public record CrearUsuarioRequest(
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,80}") String username,
    @NotBlank @Size(min = 12, max = 72) String password,
    @NotNull Rol rol) {}
