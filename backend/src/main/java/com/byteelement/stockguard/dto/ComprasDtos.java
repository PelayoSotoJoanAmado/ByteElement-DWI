package com.byteelement.stockguard.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ComprasDtos {
  private ComprasDtos() {}

  public record CrearSolicitud(
      @NotNull @Positive Long productoId,
      @NotNull @Positive Long almacenId,
      @NotNull @Positive Long cantidad,
      @NotBlank @Size(max = 255) String motivo,
      @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal montoEstimado,
      @NotBlank @Size(max = 80) String claveIdempotencia,
      boolean urgente,
      @Size(max = 255) String motivoUrgencia) {}

  public record Aprobar(@Size(max = 500) String justificacion) {}

  public record Motivo(@NotBlank @Size(max = 500) String motivo) {}

  public record Duplicidad(
      @NotNull @Positive Long originalId, @NotBlank @Size(max = 500) String motivo) {}

  public record Recibir(
      @NotNull @Positive Long cantidad,
      @NotBlank @Size(max = 500) String motivo,
      @NotBlank @Size(max = 80) String claveIdempotencia) {}

  public record Solicitud(
      Long id,
      Long productoId,
      String sku,
      Long almacenId,
      long cantidad,
      String motivo,
      BigDecimal montoEstimado,
      String estado,
      Long solicitanteId,
      Instant enviadaEn,
      Instant aprobadaEn,
      Instant revisadaEn,
      Long revisadoPorId,
      String motivoRechazo,
      String justificacionAprobacion,
      boolean urgente,
      String motivoUrgencia,
      Long duplicadaDeId,
      Instant duplicidadConfirmadaEn,
      Long duplicidadConfirmadaPorId,
      String motivoDuplicidad,
      List<Long> similaresAbiertas) {}

  public record Orden(
      Long id,
      Long solicitudId,
      String numeroOrden,
      String estado,
      boolean urgente,
      String motivoUrgencia,
      BigDecimal montoEstimado,
      Instant emitidaEn,
      Long emitidaPorId,
      long cantidad,
      long cantidadRecibida,
      long cantidadPendiente,
      Instant canceladaEn,
      String motivoCancelacion) {}

  public record Recepcion(
      Long id,
      Long ordenId,
      Long movimientoId,
      long cantidad,
      String motivo,
      Instant fecha,
      Long usuarioId) {}

  public record Reposicion(
      Long productoId,
      String sku,
      Long almacenId,
      long stock,
      long minimo,
      long maximo,
      long consumo,
      BigDecimal consumoDiario,
      long pedidosPendientes,
      long posicionInventario,
      long puntoReposicion,
      long cantidadSugerida,
      String estado,
      boolean urgenciaSugerida,
      Instant desde,
      Instant hasta,
      int ventanaDias,
      int plazoDias,
      int seguridadDias,
      String regla) {}
}
