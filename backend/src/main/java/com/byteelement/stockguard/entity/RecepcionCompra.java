package com.byteelement.stockguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recepcion_compra")
public class RecepcionCompra {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private OrdenCompra orden;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Usuario usuario;

  private Long movimientoId;
  private long cantidad;

  @Column(length = 500)
  private String motivo;

  private Instant fecha;

  @Column(length = 80)
  private String claveIdempotencia;

  protected RecepcionCompra() {}

  public RecepcionCompra(
      OrdenCompra o, Usuario u, Long m, long c, String motivo, Instant f, String clave) {
    orden = o;
    usuario = u;
    movimientoId = m;
    cantidad = c;
    this.motivo = motivo;
    fecha = f;
    claveIdempotencia = clave;
  }

  public Long getId() {
    return id;
  }

  public OrdenCompra getOrden() {
    return orden;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public Long getMovimientoId() {
    return movimientoId;
  }

  public long getCantidad() {
    return cantidad;
  }

  public String getMotivo() {
    return motivo;
  }

  public Instant getFecha() {
    return fecha;
  }
}
