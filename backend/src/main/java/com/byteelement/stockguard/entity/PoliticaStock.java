package com.byteelement.stockguard.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "politica_stock", uniqueConstraints =
        @UniqueConstraint(name = "uk_politica_producto_almacen", columnNames = {"producto_id", "almacen_id"}))
public class PoliticaStock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;
    @Column(nullable = false)
    private long minimo;
    @Column(nullable = false)
    private long maximo;

    protected PoliticaStock() {}
    public PoliticaStock(Producto producto, Almacen almacen, long minimo, long maximo) {
        this.producto = producto;
        this.almacen = almacen;
        actualizar(minimo, maximo);
    }
    public void actualizar(long minimo, long maximo) {
        if (minimo < 0 || maximo <= minimo) {
            throw new IllegalArgumentException("El minimo debe ser >= 0 y el maximo debe ser mayor que el minimo.");
        }
        this.minimo = minimo;
        this.maximo = maximo;
    }
    public Long getId() { return id; }
    public Producto getProducto() { return producto; }
    public Almacen getAlmacen() { return almacen; }
    public long getMinimo() { return minimo; }
    public long getMaximo() { return maximo; }
}
