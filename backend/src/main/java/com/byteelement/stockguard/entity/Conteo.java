package com.byteelement.stockguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "conteo")
public class Conteo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "producto_id")
  private Producto producto;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "almacen_id")
  private Almacen almacen;

  private long movimientoCorteId;
  private long cantidadFisica;
  private long saldoCorte;
  private Instant fechaCorte;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(length = 100)
  private String autor;

  @Column(length = 500)
  private String motivo;

  private boolean confirmado;

  @Column(length = 100)
  private String confirmadoPor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "confirmado_por_id")
  private Usuario confirmadoPorUsuario;

  private Instant confirmadoEn;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "movimiento_id")
  private MovimientoPersistido movimiento;

  protected Conteo() {}

  public Conteo(
      Producto p,
      Almacen a,
      long fisica,
      long saldo,
      long movimientoCorteId,
      Instant fecha,
      Usuario usuario,
      String motivo) {
    this.producto = p;
    this.almacen = a;
    this.movimientoCorteId = movimientoCorteId;
    this.cantidadFisica = fisica;
    this.saldoCorte = saldo;
    this.fechaCorte = fecha;
    this.usuario = java.util.Objects.requireNonNull(usuario, "Usuario requerido");
    this.autor = usuario.getUsername();
    this.motivo = motivo;
  }

  public void confirmar(Usuario actor, Instant fecha, MovimientoPersistido movimiento) {
    this.confirmado = true;
    this.confirmadoPorUsuario = java.util.Objects.requireNonNull(actor, "Supervisor requerido");
    this.confirmadoPor = actor.getUsername();
    this.confirmadoEn = fecha;
    this.movimiento = movimiento;
  }

  public long getMovimientoCorteId() {
    return movimientoCorteId;
  }

  public Long getUsuarioId() {
    return usuario.getId();
  }

  public Long getId() {
    return id;
  }

  public Producto getProducto() {
    return producto;
  }

  public Almacen getAlmacen() {
    return almacen;
  }

  public long getCantidadFisica() {
    return cantidadFisica;
  }

  public long getSaldoCorte() {
    return saldoCorte;
  }

  public Instant getFechaCorte() {
    return fechaCorte;
  }

  public String getAutor() {
    return autor;
  }

  public String getMotivo() {
    return motivo;
  }

  public boolean isConfirmado() {
    return confirmado;
  }

  public Long getConfirmadoPorId() {
    return confirmadoPorUsuario == null ? null : confirmadoPorUsuario.getId();
  }

  public String getConfirmadoPor() {
    return confirmadoPor;
  }

  public Instant getConfirmadoEn() {
    return confirmadoEn;
  }

  public Long getMovimientoId() {
    return movimiento == null ? null : movimiento.getId();
  }
}
