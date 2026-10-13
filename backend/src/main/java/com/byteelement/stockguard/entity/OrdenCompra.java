package com.byteelement.stockguard.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "orden_compra")
public class OrdenCompra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
  private SolicitudCompra solicitud;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "numero_orden", nullable = false, unique = true, length = 50)
  private String numeroOrden;

  @Column(nullable = false)
  private boolean urgente;

  @Column(name = "motivo_urgencia", length = 255)
  private String motivoUrgencia;

  @Column(name = "emitida_en", nullable = false)
  private Instant emitidaEn;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EstadoOrdenCompra estado = EstadoOrdenCompra.EMITIDA;

  @Column(name = "monto_estimado", nullable = false, precision = 12, scale = 2)
  private BigDecimal montoEstimado = BigDecimal.ZERO;

  protected OrdenCompra() {}

  public OrdenCompra(
      SolicitudCompra solicitud,
      Usuario usuario,
      String numeroOrden,
      boolean urgente,
      String motivoUrgencia,
      Instant emitidaEn,
      BigDecimal montoEstimado) {
    this.solicitud = Objects.requireNonNull(solicitud, "Solicitud es requerida");
    this.usuario = Objects.requireNonNull(usuario, "Usuario es requerido");
    this.numeroOrden = Objects.requireNonNull(numeroOrden, "Numero de orden es requerido");
    this.urgente = urgente;
    this.motivoUrgencia = motivoUrgencia;
    this.emitidaEn = Objects.requireNonNull(emitidaEn, "Fecha emitida es requerida");
    this.montoEstimado = montoEstimado != null ? montoEstimado : BigDecimal.ZERO;
    this.estado = EstadoOrdenCompra.EMITIDA;
  }

  public void marcarAtendida() {
    this.estado = EstadoOrdenCompra.ATENDIDA;
  }

  public void cancelar() {
    this.estado = EstadoOrdenCompra.CANCELADA;
  }

  public Long getId() {
    return id;
  }

  public SolicitudCompra getSolicitud() {
    return solicitud;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public String getNumeroOrden() {
    return numeroOrden;
  }

  public boolean isUrgente() {
    return urgente;
  }

  public String getMotivoUrgencia() {
    return motivoUrgencia;
  }

  public Instant getEmitidaEn() {
    return emitidaEn;
  }

  public EstadoOrdenCompra getEstado() {
    return estado;
  }

  public BigDecimal getMontoEstimado() {
    return montoEstimado;
  }
}

