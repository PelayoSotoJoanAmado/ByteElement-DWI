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

    if (lista.isEmpty()) {
      return new PageImpl<>(List.of(), page, total);
    }

    Set<Long> pIds =
        lista.stream().map(ps -> ps.getProducto().getId()).collect(java.util.stream.Collectors.toSet());
    Set<Long> aIds =
        lista.stream().map(ps -> ps.getAlmacen().getId()).collect(java.util.stream.Collectors.toSet());

    Map<String, Long> stockMap = new HashMap<>();
    List<Object[]> existenciasRows =
        em.createQuery(
                "select e.producto.id, e.almacen.id, e.cantidad from Existencia e where e.producto.id in :pIds and e.almacen.id in :aIds",
                Object[].class)
            .setParameter("pIds", pIds)
            .setParameter("aIds", aIds)
            .getResultList();
    for (Object[] row : existenciasRows) {
      stockMap.put(row[0] + ":" + row[1], (Long) row[2]);
    }

    Map<String, Long> consumoMap = new HashMap<>();
    List<Object[]> consumoRows =
        em.createQuery(
                "select m.producto.id, m.almacen.id, coalesce(sum(m.cantidad),0) from MovimientoPersistido m where"
                    + " m.tipo=:tipo and m.fecha>=:d and m.fecha<:h and m.producto.id in :pIds and m.almacen.id in :aIds"
                    + " group by m.producto.id, m.almacen.id",
                Object[].class)
            .setParameter("tipo", TipoMovimiento.SALIDA)
            .setParameter("d", desde)
            .setParameter("h", hasta)
            .setParameter("pIds", pIds)
            .setParameter("aIds", aIds)
            .getResultList();
    for (Object[] row : consumoRows) {
      consumoMap.put(row[0] + ":" + row[1], (Long) row[2]);
    }

    Map<String, Long> pendientesMap = new HashMap<>();
    List<Object[]> pendientesRows =
        em.createQuery(
                "select o.solicitud.producto.id, o.solicitud.almacen.id, coalesce(sum(o.solicitud.cantidad-o.cantidadRecibida),0) from OrdenCompra o where"
                    + " o.estado=com.byteelement.stockguard.entity.EstadoOrdenCompra.EMITIDA and o.solicitud.producto.id in :pIds and o.solicitud.almacen.id in :aIds"
                    + " group by o.solicitud.producto.id, o.solicitud.almacen.id",
                Object[].class)
            .setParameter("pIds", pIds)
            .setParameter("aIds", aIds)
            .getResultList();
    for (Object[] row : pendientesRows) {
      pendientesMap.put(row[0] + ":" + row[1], (Long) row[2]);
    }

    return new PageImpl<>(
        lista.stream().map(ps -> calcular(ps, stockMap, consumoMap, pendientesMap, desde, hasta)).toList(),
        page,
        total);
  }

  private Reposicion calcular(
      PoliticaStock ps,
      Map<String, Long> stockMap,
      Map<String, Long> consumoMap,
      Map<String, Long> pendientesMap,
      Instant desde,
      Instant hasta) {
    long p = ps.getProducto().getId(), a = ps.getAlmacen().getId();
    String key = p + ":" + a;
    long stock = stockMap.getOrDefault(key, 0L);
    long consumo = consumoMap.getOrDefault(key, 0L);
    BigDecimal diario =
        BigDecimal.valueOf(consumo).divide(BigDecimal.valueOf(ventana), 8, RoundingMode.HALF_UP);
    long cobertura =
        BigDecimal.valueOf(consumo)
            .multiply(BigDecimal.valueOf(plazo + seguridad))
            .divide(BigDecimal.valueOf(ventana), 0, RoundingMode.CEILING)
            .longValueExact();
    long punto = Math.min(ps.getMaximo(), Math.max(ps.getMinimo(), cobertura));
    long pendientes = pendientesMap.getOrDefault(key, 0L), posicion = Math.addExact(stock, pendientes);
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
