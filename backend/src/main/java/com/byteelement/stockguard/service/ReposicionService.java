package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.ComprasDtos.Reposicion;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.repository.*;
import jakarta.persistence.EntityManager;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReposicionService {
  private final EntityManager em;
  private final ExistenciaRepository existencias;
  private final OrdenCompraRepository ordenes;
  private final int ventana, plazo, seguridad;

  public ReposicionService(
      EntityManager em,
      ExistenciaRepository e,
      OrdenCompraRepository o,
      @Value("${stockguard.reposicion.ventana-dias:30}") int v,
      @Value("${stockguard.reposicion.plazo-dias:7}") int p,
      @Value("${stockguard.reposicion.seguridad-dias:3}") int s) {
    if (v < 1 || p < 1 || s < 0 || v > 365 || p > 365 || s > 365)
      throw new IllegalArgumentException("Parametros de reposicion invalidos");
    this.em = em;
    existencias = e;
    ordenes = o;
    ventana = v;
    plazo = p;
    seguridad = s;
  }

  public Page<Reposicion> listar(String sku, Long almacenId, int pagina, int tamanio) {
    String filtro =
        " from PoliticaStock ps where ps.producto.activo=true and ps.almacen.activo=true and (:sku"
            + " is null or ps.producto.sku=:sku) and (:a is null or ps.almacen.id=:a)";
    String normal = sku == null ? null : sku.trim().toUpperCase(Locale.ROOT);
    var page = PageRequest.of(pagina, tamanio);
    var lista =
        em.createQuery(
                "select ps" + filtro + " order by ps.producto.id,ps.almacen.id",
                PoliticaStock.class)
            .setParameter("sku", normal)
            .setParameter("a", almacenId)
            .setFirstResult(Math.toIntExact(page.getOffset()))
            .setMaxResults(tamanio)
            .getResultList();
    long total =
        em.createQuery("select count(ps)" + filtro, Long.class)
            .setParameter("sku", normal)
            .setParameter("a", almacenId)
            .getSingleResult();
    Instant hasta = Instant.now(), desde = hasta.minus(ventana, java.time.temporal.ChronoUnit.DAYS);
    return new PageImpl<>(
        lista.stream().map(ps -> calcular(ps, desde, hasta)).toList(), page, total);
  }

  private Reposicion calcular(PoliticaStock ps, Instant desde, Instant hasta) {
    long p = ps.getProducto().getId(), a = ps.getAlmacen().getId();
    long stock =
        existencias.findByProductoIdAndAlmacenId(p, a).map(Existencia::getCantidad).orElse(0L);
    long consumo =
        em.createQuery(
                "select coalesce(sum(m.cantidad),0) from MovimientoPersistido m where"
                    + " m.producto.id=:p and m.almacen.id=:a and m.tipo=:tipo and m.fecha>=:d and"
                    + " m.fecha<:h",
                Long.class)
            .setParameter("p", p)
            .setParameter("a", a)
            .setParameter("tipo", TipoMovimiento.SALIDA)
            .setParameter("d", desde)
            .setParameter("h", hasta)
            .getSingleResult();
    BigDecimal diario =
        BigDecimal.valueOf(consumo).divide(BigDecimal.valueOf(ventana), 8, RoundingMode.HALF_UP);
    long cobertura =
        BigDecimal.valueOf(consumo)
            .multiply(BigDecimal.valueOf(plazo + seguridad))
            .divide(BigDecimal.valueOf(ventana), 0, RoundingMode.CEILING)
            .longValueExact();
    long punto = Math.min(ps.getMaximo(), Math.max(ps.getMinimo(), cobertura));
    long pendientes = ordenes.pendientes(p, a), posicion = Math.addExact(stock, pendientes);
    long sugerida = posicion <= punto ? Math.max(0, ps.getMaximo() - posicion) : 0;
    boolean urgente =
        stock == 0
            || BigDecimal.valueOf(stock)
                    .multiply(BigDecimal.valueOf(ventana))
                    .compareTo(BigDecimal.valueOf(consumo).multiply(BigDecimal.valueOf(plazo)))
                < 0;
    String estado =
        stock == 0
            ? "QUIEBRE"
            : stock < ps.getMinimo()
                ? "BAJO_MINIMO"
                : stock > ps.getMaximo() ? "SOBRE_MAXIMO" : "NORMAL";
    return new Reposicion(
        p,
        ps.getProducto().getSku(),
        a,
        stock,
        ps.getMinimo(),
        ps.getMaximo(),
        consumo,
        diario,
        pendientes,
        posicion,
        punto,
        sugerida,
        estado,
        urgente,
        desde,
        hasta,
        ventana,
        plazo,
        seguridad,
        "MIN_MAX_CONSUMO_V1: punto=min(maximo,max(minimo,ceil(consumo/ventana*(plazo+seguridad))));"
            + " si stock+pendientes<=punto, sugerida=max(0,maximo-stock-pendientes). Sin salidas"
            + " registradas se aplica minimo/maximo; no acredita historial completo.");
  }
}
