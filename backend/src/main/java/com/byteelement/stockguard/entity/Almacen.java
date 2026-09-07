package com.byteelement.stockguard.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "almacen")
public class Almacen {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(nullable = false)
    private boolean activo = true;

    protected Almacen() {}
    public Almacen(String codigo, String nombre) { actualizar(codigo, nombre); }
    public void actualizar(String codigo, String nombre) { this.codigo = codigo; this.nombre = nombre; }
    public void desactivar() { activo = false; }
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
}
