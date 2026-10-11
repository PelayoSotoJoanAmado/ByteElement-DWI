package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.repository.*;
import com.byteelement.stockguard.seguridad.ActorActual;
import java.math.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReporteService {
  private final ProductoRepository productos;
  private final AlmacenRepository almacenes;
  private final ExistenciaRepository existencias;
  private final MovimientoRepository movimientos;
  private final PoliticaStockRepository politicas;
  private final CorteStockRepository cortes;
  private final ConteoRepository conteos;
  private final ActorActual actor;

  public ReporteService(
      ProductoRepository p,
      AlmacenRepository a,
      ExistenciaRepository e,
      MovimientoRepository m,
      PoliticaStockRepository ps,
      CorteStockRepository c,
      ConteoRepository co,
      ActorActual actor) {
    productos = p;
    almacenes = a;
    existencias = e;
    movimientos = m;
    politicas = ps;
    cortes = c;
    conteos = co;
    this.actor = actor;
  }

  // Fotografiar catálogo activo, todos sus almacenes activos (también saldos cero) y límites
  // actuales.
  @Transactional
  public List<CorteStockResponse> crearCorte() {
    var ps = productos.bloquearTodos().stream().filter(Producto::isActivo).toList();
    var as = almacenes.bloquearTodos().stream().filter(Almacen::isActivo).toList();
    var resultado = new ArrayList<CorteStock>();
    String lote = UUID.randomUUID().toString();
    Instant fecha = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    Usuario usuario = actor.usuario();
    for (var p : ps)
      for (var a : as) {
        long saldo =
            existencias
                .findByProductoIdAndAlmacenId(p.getId(), a.getId())
                .map(Existencia::getCantidad)
                .orElse(0L);
        var politica = politicas.findByProductoIdAndAlmacenId(p.getId(), a.getId()).orElse(null);
        resultado.add(new CorteStock(lote, fecha, usuario, p, a, saldo, politica));
      }
    return cortes.saveAllAndFlush(resultado).stream().map(CorteStockResponse::desde).toList();
  }

  public List<CorteStockResponse> obtenerCorte(String lote) {
    return cortes.lote(lote).stream().map(CorteStockResponse::desde).toList();
  }

  private List<CorteStock> ultimoCorte(Instant hasta) {
    var ultimo = cortes.ultimo(hasta, PageRequest.of(0, 1));
    return ultimo.isEmpty() ? List.of() : cortes.lote(ultimo.getFirst().getLote());
  }

  private static String par(Long p, Long a) {
    return p + ":" + a;
  }

  private long saldoAntes(Long p, Long a, Instant hasta) {
    var ultimo = movimientos.ultimo(p, a, hasta, PageRequest.of(0, 1));
    return ultimo.isEmpty() ? 0L : ultimo.getFirst().getSaldoResultante();
  }

  public IndicadorResponse indicador(
      String codigo, Instant desde, Instant hasta, String sku, Long almacenId) {
    Periodo.validar(desde, hasta);
    String skuNormal = sku == null ? null : sku.trim().toUpperCase(Locale.ROOT);
    long n = 0, d = 0;
    String nombre, lote = null;
    if (codigo.equals("I07")) {
      var lista =
          conteos.periodo(desde, hasta).stream()
              .filter(
                  c ->
                      (skuNormal == null || c.getProducto().getSku().equals(skuNormal))
                          && (almacenId == null || c.getAlmacen().getId().equals(almacenId)))
              .toList();
      // Cada registro es una observación producto/almacén/fecha de corte, no el saldo actual.
      d = lista.size();
      n = lista.stream().filter(c -> c.getSaldoCorte() == c.getCantidadFisica()).count();
      nombre = "Coincidencia de inventario";
    } else if (codigo.equals("I05") || codigo.equals("I10")) {
      var cierre = ultimoCorte(hasta);
      // Sin corte dentro del período no inferimos la población histórica del catálogo.
      if (!cierre.isEmpty() && cierre.getFirst().getFecha().isBefore(desde)) cierre = List.of();
      var observados =
          cierre.stream()
              .filter(
                  c ->
                      c.isCritico()
                          && (skuNormal == null || c.getSku().equals(skuNormal))
                          && (almacenId == null || c.getAlmacenId().equals(almacenId)))
              .toList();
      d = observados.stream().map(CorteStock::getProductoId).distinct().count();
      if (!cierre.isEmpty()) lote = cierre.getFirst().getLote();
      Set<Long> afectados = new HashSet<>();
      if (codigo.equals("I05")) {
        Set<String> pares = new HashSet<>();
        for (var c : observados) {
          pares.add(par(c.getProductoId(), c.getAlmacenId()));
          if (saldoAntes(c.getProductoId(), c.getAlmacenId(), desde) == 0)
            afectados.add(c.getProductoId());
        }
        for (var m : movimientos.periodo(desde, hasta)) {
          if (m.getSaldoResultante() == 0
              && pares.contains(par(m.getProducto().getId(), m.getAlmacen().getId())))
            afectados.add(m.getProducto().getId());
        }
        nombre = "Quiebre en el período";
      } else {
        // Un máximo faltante no equivale a cero: no se acepta como medición completa.
        if (observados.stream().anyMatch(c -> c.getMaximo() == null))
          throw new IllegalArgumentException(
              "I10 requiere política para cada par crítico del corte");
        for (var c : observados)
          if (c.getCantidad() > c.getMaximo()) afectados.add(c.getProductoId());
        nombre = "Productos sobre el máximo al corte";
      }
      n = afectados.size();
    } else
      throw new IllegalArgumentException("APF2 implementa I05, I07 e I10; código no disponible");
    BigDecimal valor =
        d == 0
            ? null
            : BigDecimal.valueOf(n)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(d), 2, RoundingMode.HALF_UP);
    return new IndicadorResponse(codigo, nombre, "%", n, d, valor, d == 0, desde, hasta, lote);
  }

  public Page<Map<String, Object>> stockCritico(
      String sku, Long almacenId, Instant desde, Instant hasta, int pagina, int tamanio) {
    Periodo.validar(desde, hasta);
    var lista =
        ultimoCorte(hasta).stream()
            .filter(
                c ->
                    c.isCritico()
                        && !c.getFecha().isBefore(desde)
                        && (sku == null || c.getSku().equals(sku.trim().toUpperCase(Locale.ROOT)))
                        && (almacenId == null || c.getAlmacenId().equals(almacenId)))
            .map(
                c -> {
                  Map<String, Object> r = new LinkedHashMap<>();
                  r.put("productoId", c.getProductoId());
                  r.put("sku", c.getSku());
                  r.put("almacenId", c.getAlmacenId());
                  r.put("cantidad", c.getCantidad());
                  r.put("minimo", c.getMinimo());
                  r.put("maximo", c.getMaximo());
                  r.put(
                      "estado",
                      c.getCantidad() == 0
                          ? "QUIEBRE"
                          : c.getMinimo() == null
                              ? "SIN_POLITICA"
                              : c.getCantidad() < c.getMinimo()
                                  ? "BAJO_MINIMO"
                                  : c.getCantidad() > c.getMaximo() ? "SOBRE_MAXIMO" : "NORMAL");
                  r.put("fechaCorte", c.getFecha());
                  r.put("lote", c.getLote());
                  return r;
                })
            .toList();
    return paginar(lista, pagina, tamanio);
  }

  public Page<Map<String, Object>> rotacion(
      String sku, Long almacenId, Instant desde, Instant hasta, int pagina, int tamanio) {
    Periodo.validar(desde, hasta);
    // Consumo comercial: SALIDA; transferencias y ajustes no representan ventas.
    var pares =
        movimientos
            .buscar(
                null,
                sku == null ? null : sku.trim().toUpperCase(Locale.ROOT),
                almacenId,
                Instant.parse("1970-01-01T00:00:00Z"),
                hasta,
                Pageable.unpaged())
            .getContent();
    Map<String, MovimientoPersistido> claves = new TreeMap<>();
    for (var m : pares) claves.put(par(m.getProducto().getId(), m.getAlmacen().getId()), m);
    List<Map<String, Object>> lista = new ArrayList<>();
    for (var m : claves.values()) {
      long p = m.getProducto().getId(), a = m.getAlmacen().getId();
      var historial =
          pares.stream()
              .filter(x -> x.getProducto().getId() == p && x.getAlmacen().getId() == a)
              .filter(x -> !x.getFecha().isBefore(desde))
              .sorted(
                  Comparator.comparing(MovimientoPersistido::getFecha)
                      .thenComparing(MovimientoPersistido::getId))
              .toList();
      long saldo = saldoAntes(p, a, desde), consumo = 0;
      Instant inicio = desde;
      double area = 0;
      for (var x : historial) {
        area += saldo * (segundos(inicio, x.getFecha()));
        inicio = x.getFecha();
        saldo = x.getSaldoResultante();
        if (x.getTipo() == TipoMovimiento.SALIDA) consumo = Math.addExact(consumo, x.getCantidad());
      }
      area += saldo * (segundos(inicio, hasta));
      double promedio = area / (segundos(desde, hasta));
      Map<String, Object> r = new LinkedHashMap<>();
      r.put("productoId", p);
      r.put("sku", m.getProducto().getSku());
      r.put("almacenId", a);
      r.put("consumo", consumo);
      r.put("stockPromedio", BigDecimal.valueOf(promedio).setScale(4, RoundingMode.HALF_UP));
      r.put(
          "rotacion",
          promedio == 0
              ? null
              : BigDecimal.valueOf(consumo / promedio).setScale(4, RoundingMode.HALF_UP));
      r.put("desde", desde);
      r.put("hasta", hasta);
      lista.add(r);
    }
    return paginar(lista, pagina, tamanio);
  }

  private static double segundos(Instant a, Instant b) {
    var d = java.time.Duration.between(a, b);
    return d.getSeconds() + d.getNano() / 1e9;
  }

  private <T> Page<T> paginar(List<T> lista, int pagina, int tamanio) {
    var page = PageRequest.of(pagina, tamanio);
    long offset = page.getOffset();
    List<T> contenido =
        offset >= lista.size()
            ? List.of()
            : lista.subList((int) offset, Math.min(lista.size(), (int) offset + tamanio));
    return new PageImpl<>(contenido, page, lista.size());
  }
}
