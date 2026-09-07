package com.byteelement.stockguard.dto;

public record AlmacenResponse(long id, String codigo, String nombre, boolean activo) {
    public AlmacenResponse(long id, String codigo, String nombre) { this(id, codigo, nombre, true); }
    public static AlmacenResponse desde(com.byteelement.stockguard.entity.Almacen almacen) {
        return new AlmacenResponse(almacen.getId(), almacen.getCodigo(), almacen.getNombre(), almacen.isActivo());
    }
}
