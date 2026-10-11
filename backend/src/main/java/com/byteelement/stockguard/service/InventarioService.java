package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.exception.*;
import com.byteelement.stockguard.repository.*;
import com.byteelement.stockguard.seguridad.ActorActual;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventarioService {
  private final ProductoRepository productos;
  private final AlmacenRepository almacenes;
  private final ExistenciaRepository existencias;
  private final MovimientoRepository movimientos;
  private final ConteoRepository conteos;
  private final ActorActual actor;

  public InventarioService(
      ProductoRepository productos,
      AlmacenRepository almacenes,
      ExistenciaRepository existencias,
      MovimientoRepository movimientos,
      ConteoRepository conteos,
      ActorActual actor) {
    this.productos = productos;
    this.almacenes = almacenes;
    this.existencias = existencias;
    this.movimientos = movimientos;
    this.conteos = conteos;
    this.actor = actor;
  }

  // El bloqueo del producto existe incluso cuando el saldo del par aún no existe.
  // Serializa operaciones del mismo SKU entre instancias y evita carreras al crear saldos.
  private Producto productoBloqueado(Long id) {
    Producto p =
        productos
            .bloquear(id)
            .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));
    if (!p.isActivo()) throw new ConflictoCatalogoException("El producto esta desactivado.");
    return p;
  }

  private Almacen almacenBloqueado(Long id) {
    Almacen a =
        almacenes
            .bloquear(id)
            .orElseThrow(() -> new NoSuchElementException("Almacen no encontrado"));
    if (!a.isActivo()) throw new ConflictoCatalogoException("El almacen esta desactivado.");
    return a;
  }

  private Existencia saldo(Producto p, Almacen a) {
    return existencias
        .findByProductoIdAndAlmacenId(p.getId(), a.getId())
        .orElseGet(() -> existencias.save(new Existencia(p, a)));
  }

  private long sumar(long saldo, long cantidad) {
    try {
      return Math.addExact(saldo, cantidad);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("La cantidad supera la capacidad numerica del saldo.");
    }
  }

  private MovimientoPersistido guardar(
      Producto p,
      Almacen a,
      TipoMovimiento tipo,
      long cantidad,
      String motivo,
      long saldo,
      String referencia,
      Instant fecha) {
    return movimientos.saveAndFlush(
        new MovimientoPersistido(
            p, a, tipo, cantidad, actor.usuario(), fecha, motivo, saldo, referencia));
  }

  @Transactional
  public Movimiento registrar(RegistrarMovimientoRequest request) {
    Producto p = productoBloqueado(request.productoId());
    Almacen a = almacenBloqueado(request.almacenId());
    if (request.cantidad() == null || request.cantidad() <= 0)
      throw new IllegalArgumentException("Cantidad debe ser positiva");
    Existencia e = saldo(p, a);
    long nuevo;
    if (request.tipo() == TipoMovimiento.ENTRADA)
      nuevo = sumar(e.getCantidad(), request.cantidad());
    else if (request.tipo() == TipoMovimiento.SALIDA) {
      if (e.getCantidad() < request.cantidad()) throw new StockInsuficienteException();
      nuevo = e.getCantidad() - request.cantidad();
    } else throw new IllegalArgumentException("Tipo de movimiento no soportado");
    e.cambiar(nuevo);
    return guardar(
            p,
            a,
            request.tipo(),
            request.cantidad(),
            request.motivo(),
            nuevo,
            UUID.randomUUID().toString(),
            Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS))
        .modelo();
  }

  @Transactional
  public TransferenciaResponse transferir(RegistrarTransferenciaRequest request) {
    if (request.origenAlmacenId().equals(request.destinoAlmacenId()))
      throw new IllegalArgumentException("El almacén de origen y destino no pueden ser el mismo.");
    if (request.cantidad() == null || request.cantidad() <= 0)
      throw new IllegalArgumentException("Cantidad debe ser positiva");
    Producto p = productoBloqueado(request.productoId());
    Map<Long, Almacen> pares = new HashMap<>();
    for (Long id : new TreeSet<>(List.of(request.origenAlmacenId(), request.destinoAlmacenId())))
      pares.put(id, almacenBloqueado(id));
    Almacen origen = pares.get(request.origenAlmacenId()),
        destino = pares.get(request.destinoAlmacenId());
    Existencia eo = saldo(p, origen), ed = saldo(p, destino);
    if (eo.getCantidad() < request.cantidad()) throw new StockInsuficienteException();
    long so = eo.getCantidad() - request.cantidad(),
        sd = sumar(ed.getCantidad(), request.cantidad());
    Instant fecha = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    String referencia = UUID.randomUUID().toString();
    eo.cambiar(so);
    ed.cambiar(sd);
    var salida =
        guardar(
            p,
            origen,
            TipoMovimiento.TRANSFERENCIA_SALIDA,
            request.cantidad(),
            request.motivo(),
            so,
            referencia,
            fecha);
    var entrada =
        guardar(
            p,
            destino,
            TipoMovimiento.TRANSFERENCIA_ENTRADA,
            request.cantidad(),
            request.motivo(),
            sd,
            referencia,
            fecha);
    return new TransferenciaResponse(
        salida.getId(),
        entrada.getId(),
        p.getId(),
        origen.getId(),
        destino.getId(),
        request.cantidad(),
        so,
        sd,
        actor.nombre(),
        request.motivo(),
        fecha);
  }

  public ExistenciaResponse consultar(long productoId, long almacenId) {
    productos
        .findById(productoId)
        .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));
    almacenes
        .findById(almacenId)
        .orElseThrow(() -> new NoSuchElementException("Almacen no encontrado"));
    return new ExistenciaResponse(
        productoId,
        almacenId,
        existencias
            .findByProductoIdAndAlmacenId(productoId, almacenId)
            .map(Existencia::getCantidad)
            .orElse(0L));
  }

  public List<AlmacenResponse> almacenes() {
    return almacenes.findAll(Sort.by("id")).stream().map(AlmacenResponse::desde).toList();
  }

  public AlmacenResponse obtenerAlmacen(long id) {
    return AlmacenResponse.desde(
        almacenes
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Almacen no encontrado")));
  }

  public Movimiento obtenerMovimiento(long id) {
    return movimientos
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("Movimiento no encontrado"))
        .modelo();
  }

  public Page<MovimientoResponse> listarMovimientos(
      Long productoId,
      String sku,
      Long almacenId,
      Instant desde,
      Instant hasta,
      int pagina,
      int tamanio) {
    Periodo.validar(desde, hasta);
    return movimientos
        .buscar(
            productoId,
            sku == null ? null : sku.trim().toUpperCase(Locale.ROOT),
            almacenId,
            desde,
            hasta,
            PageRequest.of(pagina, tamanio, Sort.by("fecha", "id")))
        .map(m -> MovimientoResponse.desde(m.modelo()));
  }

  @Transactional
  public ConteoResponse contar(RegistrarConteoRequest request) {
    Producto p = productoBloqueado(request.productoId());
    Almacen a = almacenBloqueado(request.almacenId());
    var c =
        conteos.saveAndFlush(
            new Conteo(
                p,
                a,
                request.cantidadFisica(),
                saldo(p, a).getCantidad(),
                movimientos.ultimoId(p.getId(), a.getId()),
                Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                actor.usuario(),
                request.motivo()));
    return ConteoResponse.desde(c);
  }

  @Transactional
  public ConteoResponse confirmarConteo(Long id) {
    // Mantener el mismo orden producto -> almacen -> conteo en todas las mutaciones.
    var ids = conteos.ids(id);
    if (ids.isEmpty()) throw new NoSuchElementException("Conteo no encontrado");
    Producto p = productoBloqueado((Long) ids.getFirst()[0]);
    Almacen a = almacenBloqueado((Long) ids.getFirst()[1]);
    Conteo c = conteos.bloquear(id).orElseThrow();
    if (c.isConfirmado()) throw new ConflictoCatalogoException("Conteo ya confirmado");
    Existencia e = saldo(p, a);
    if (e.getCantidad() != c.getSaldoCorte()
        || movimientos.ultimoId(p.getId(), a.getId()) != c.getMovimientoCorteId())
      throw new ConflictoCatalogoException(
          "El inventario cambió desde el conteo; registre un nuevo conteo.");
    long diferencia = c.getCantidadFisica() - e.getCantidad();
    MovimientoPersistido m = null;
    Instant fecha = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    if (diferencia != 0) {
      e.cambiar(c.getCantidadFisica());
      m =
          guardar(
              p,
              a,
              diferencia > 0 ? TipoMovimiento.AJUSTE_ENTRADA : TipoMovimiento.AJUSTE_SALIDA,
              Math.abs(diferencia),
              c.getMotivo(),
              e.getCantidad(),
              "CONTEO-" + id,
              fecha);
    }
    c.confirmar(actor.usuario(), fecha, m);
    return ConteoResponse.desde(c);
  }

  public ConteoResponse obtenerConteo(Long id) {
    return ConteoResponse.desde(
        conteos.findById(id).orElseThrow(() -> new NoSuchElementException("Conteo no encontrado")));
  }
}
