package com.byteelement.stockguard.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record IndicadorResponse(
    String codigo,
    String nombre,
    String unidad,
    long numerador,
    long denominador,
    BigDecimal valor,
    boolean sinDatos,
    Instant desde,
    Instant hasta,
    String lote) {}
