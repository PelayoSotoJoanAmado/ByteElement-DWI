package com.byteelement.stockguard.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "solicitud_compra")
public class SolicitudCompra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "producto_id", nullable = false)
  private Producto producto;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "almacen_id", nullable = false)
  private Almacen almacen;

  @Column(nullable = false)
  private long cantidad;

  @Column(nullable = false, length = 255)
  private String motivo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EstadoSolicitudCompra estado = EstadoSolicitudCompra.PENDIENTE;

  @Column(name = "enviada_en", nullable = false)
  private Instant enviadaEn;

  @Column(name = "aprobada_en")
  private Instant aprobadaEn;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "revisado_por_id")
  private Usuario revisadoPor;

  @Column(name = "motivo_rechazo", length = 255)
  private String motivoRechazo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "duplicada_de_id")
  private SolicitudCompra duplicadaDe;

  @Column(name = "clave_idempotencia", length = 80)
  private String claveIdempotencia;

  protected SolicitudCompra() {}

  public SolicitudCompra(
      Usuario usuario,
      Producto producto,
      Almacen almacen,
      long cantidad,
      String motivo,
      Instant enviadaEn,
      String claveIdempotencia) {
    if (cantidad <= 0) {
      throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
    }
    this.usuario = Objects.requireNonNull(usuario, "Usuario es requerido");
    this.producto = Objects.requireNonNull(producto, "Producto es requerido");
    this.almacen = Objects.requireNonNull(almacen, "Almacen es requerido");
    this.cantidad = cantidad;
    this.motivo = Objects.requireNonNull(motivo, "Motivo es requerido");
    this.enviadaEn = Objects.requireNonNull(enviadaEn, "Fecha enviada es requerida");
    this.estado = EstadoSolicitudCompra.PENDIENTE;
    this.claveIdempotencia = claveIdempotencia;
  }

  public void aprobar(Usuario supervisor, Instant aprobadaEn) {
    if (this.estado != EstadoSolicitudCompra.PENDIENTE) {
      throw new IllegalStateException("Solo se pueden aprobar solicitudes en estado PENDIENTE.");
    }
    this.estado = EstadoSolicitudCompra.APROBADA;
    this.revisadoPor = Objects.requireNonNull(supervisor, "Supervisor es requerido");
    this.aprobadaEn = Objects.requireNonNull(aprobadaEn, "Fecha aprobada es requerida");
  }

  public void rechazar(Usuario supervisor, String motivoRechazo, Instant fecha) {
    if (this.estado != EstadoSolicitudCompra.PENDIENTE) {
      throw new IllegalStateException("Solo se pueden rechazar solicitudes en estado PENDIENTE.");
    }
    this.estado = EstadoSolicitudCompra.RECHAZADA;
    this.revisadoPor = Objects.requireNonNull(supervisor, "Supervisor es requerido");
    this.motivoRechazo = motivoRechazo;
    this.aprobadaEn = fecha;
  }

  public void marcarDuplicada(SolicitudCompra original) {
    this.duplicadaDe = original;
  }

  public Long getId() {
    return id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public Producto getProducto() {
    return producto;
  }

  public Almacen getAlmacen() {
    return almacen;
  }

  public long getCantidad() {
    return cantidad;
  }

  public String getMotivo() {
    return motivo;
  }

  public EstadoSolicitudCompra getEstado() {
    return estado;
  }

  public Instant getEnviadaEn() {
    return enviadaEn;
  }

  public Instant getAprobadaEn() {
    return aprobadaEn;
  }

  public Usuario getRevisadoPor() {
    return revisadoPor;
  }

  public String getMotivoRechazo() {
    return motivoRechazo;
  }

  public SolicitudCompra getDuplicadaDe() {
    return duplicadaDe;
  }

  public String getClaveIdempotencia() {
    return claveIdempotencia;
  }
}

