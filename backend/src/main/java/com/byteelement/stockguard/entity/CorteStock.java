package com.byteelement.stockguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "corte_stock")
public class CorteStock {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 36)
  private String lote;

  private Instant fecha;

  @Column(length = 100)
  private String autor;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "producto_id")
  private Producto producto;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "almacen_id")
  private Almacen almacen;

  @Column(length = 50)
  private String sku;

  private boolean critico;
  private long cantidad;
  private Long minimo;
  private Long maximo;

  protected CorteStock() {}

  public CorteStock(
      String lote,
      Instant fecha,
      String autor,
      Producto p,
      Almacen a,
      long cantidad,
      PoliticaStock politica) {
    this.lote = lote;
    this.fecha = fecha;
    this.autor = autor;
    this.producto = p;
    this.almacen = a;
    this.sku = p.getSku();
    this.critico = p.isCritico();
    this.cantidad = cantidad;
    if (politica != null) {
      this.minimo = politica.getMinimo();
      this.maximo = politica.getMaximo();
    }
  }

  public Long getId() {
    return id;
  }

  public String getLote() {
    return lote;
  }

  public Instant getFecha() {
    return fecha;
  }

  public Long getProductoId() {
    return producto.getId();
  }

  public Long getAlmacenId() {
    return almacen.getId();
  }

  public String getSku() {
    return sku;
  }

  public boolean isCritico() {
    return critico;
  }

  public long getCantidad() {
    return cantidad;
  }

  public Long getMinimo() {
    return minimo;
  }

  public Long getMaximo() {
    return maximo;
  }
}
