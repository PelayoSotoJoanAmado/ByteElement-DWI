package com.byteelement.stockguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "movimiento")
public class MovimientoPersistido {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "producto_id")
  private Producto producto;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "almacen_id")
  private Almacen almacen;

  @Enumerated(EnumType.STRING)
  @Column(length = 30, nullable = false)
  private TipoMovimiento tipo;

  private long cantidad;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(length = 100, nullable = false)
  private String autor;

  @Column(nullable = false)
  private Instant fecha;

  @Column(length = 500, nullable = false)
  private String motivo;

  @Column(name = "saldo_resultante")
  private long saldoResultante;

  @Column(length = 80, nullable = false)
  private String referencia;

  protected MovimientoPersistido() {}

  public MovimientoPersistido(
      Producto p,
      Almacen a,
      TipoMovimiento tipo,
      long cantidad,
      Usuario usuario,
      Instant fecha,
      String motivo,
      long saldo,
      String referencia) {
    this.producto = p;
    this.almacen = a;
    this.tipo = tipo;
    this.cantidad = cantidad;
    this.usuario = java.util.Objects.requireNonNull(usuario, "Usuario requerido");
    this.autor = usuario.getUsername();
    this.fecha = fecha;
    this.motivo = motivo;
    this.saldoResultante = saldo;
    this.referencia = referencia;
  }

  public Movimiento modelo() {
    return new Movimiento(
        id,
        producto.getId(),
        almacen.getId(),
        tipo,
        cantidad,
        autor,
        fecha,
        motivo,
        saldoResultante);
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

  public long getCantidad() {
    return cantidad;
  }

  public Instant getFecha() {
    return fecha;
  }

  public long getSaldoResultante() {
    return saldoResultante;
  }

  public TipoMovimiento getTipo() {
    return tipo;
  }

  public String getReferencia() {
    return referencia;
  }
}
