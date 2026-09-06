package com.byteelement.stockguard.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(nullable = false, length = 80)
    private String marca;

    @Column(nullable = false, length = 120)
    private String modelo;

    @Column(nullable = false, length = 500)
    private String especificacion;

    @Column(nullable = false)
    private boolean critico;

    @Column(nullable = false)
    private boolean activo = true;

    protected Producto() {
    }

    public Producto(
            String sku,
            String categoria,
            String marca,
            String modelo,
            String especificacion,
            boolean critico) {
        this.sku = sku;
        this.categoria = categoria;
        this.marca = marca;
        this.modelo = modelo;
        this.especificacion = especificacion;
        this.critico = critico;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getCategoria() { return categoria; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getEspecificacion() { return especificacion; }
    public boolean isCritico() { return critico; }
    public boolean isActivo() { return activo; }
}