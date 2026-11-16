package com.byteelement.stockguard.dto;

import com.byteelement.stockguard.entity.EventoUsuario;
import com.byteelement.stockguard.seguridad.Rol;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class CuentasDtos {
  private CuentasDtos() {}

  public record CambiarPassword(
      @NotBlank String passwordActual,
      @NotBlank @Size(min = 12, max = 72) String passwordNueva,
      @NotBlank String confirmacionPassword) {
    @Override
    public String toString() {
      return "CambiarPassword[credenciales omitidas]";
    }
  }

  public record RestablecerPassword(
      @NotBlank @Size(min = 12, max = 72) String passwordTemporal,
      @NotBlank String confirmacionPassword,
      @NotBlank @Size(max = 300) String motivo,
      @NotNull @PositiveOrZero Long version) {
    @Override
    public String toString() {
      return "RestablecerPassword[credenciales omitidas]";
    }
  }

  public record ActualizarUsuario(
      @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,80}") String username,
      @NotNull Rol rol,
      @NotNull Boolean activo,
      @NotNull @PositiveOrZero Long version,
      @NotBlank @Size(max = 300) String motivo) {}

  public record Evento(
      Long id,
      Long usuarioId,
      Long actorId,
      String actorUsername,
      String usuarioUsername,
      String tipo,
      Instant fecha,
      String detalle) {
    public static Evento desde(EventoUsuario e) {
      return new Evento(
          e.getId(),
          e.getUsuarioId(),
          e.getActorId(),
          e.getActorUsername(),
          e.getUsuarioUsername(),
          e.getTipo(),
          e.getFecha(),
          e.getDetalle());
    }
  }
}
