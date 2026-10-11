package com.byteelement.stockguard.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "existencia",
    uniqueConstraints = @UniqueConstraint(columnNames = {"producto_id", "almacen_id"}))
public class Existencia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "producto_id")
  private Producto producto;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "almacen_id")
  private Almacen almacen;

  private long cantidad;
  @Version private long version;

  protected Existencia() {}

  public Existencia(Producto producto, Almacen almacen) {
    this.producto = producto;
    this.almacen = almacen;
  }

  public void cambiar(long cantidad) {
    if (cantidad < 0) throw new IllegalArgumentException("Saldo negativo");
    this.cantidad = cantidad;
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
}
