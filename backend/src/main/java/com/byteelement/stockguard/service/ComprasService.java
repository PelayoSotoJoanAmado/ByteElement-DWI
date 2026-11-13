package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.ComprasDtos.*;
import com.byteelement.stockguard.dto.RegistrarMovimientoRequest;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.exception.ConflictoCatalogoException;
import com.byteelement.stockguard.repository.*;
import com.byteelement.stockguard.seguridad.ActorActual;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ComprasService {
  private final SolicitudCompraRepository solicitudes;
  private final OrdenCompraRepository ordenes;
  private final RecepcionCompraRepository recepciones;
  private final ProductoRepository productos;
  private final AlmacenRepository almacenes;
  private final UsuarioRepository usuarios;
  private final InventarioService inventario;
  private final ActorActual actor;
  private final tools.jackson.databind.ObjectMapper json;
  private final BigDecimal umbral;
  private final jakarta.persistence.EntityManager em;
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(ComprasService.class);

  public ComprasService(
      SolicitudCompraRepository s,
      OrdenCompraRepository o,
      RecepcionCompraRepository r,
      ProductoRepository p,
      AlmacenRepository a,
      UsuarioRepository u,
      InventarioService i,
      ActorActual actor,
      jakarta.persistence.EntityManager em,
      tools.jackson.databind.ObjectMapper json,
      @org.springframework.beans.factory.annotation.Value(
              "${stockguard.compras.umbral-justificacion:5000}")
          BigDecimal umbral) {
    solicitudes = s;
    ordenes = o;
    recepciones = r;
    productos = p;
    almacenes = a;
    usuarios = u;
    inventario = i;
    this.actor = actor;
    this.em = em;
    this.json = json;
    this.umbral = umbral;
  }

  private Instant ahora() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  private void activo(Producto p, Almacen a) {
    if (!p.isActivo() || !a.isActivo())
      throw new ConflictoCatalogoException("Producto o almacen inactivo");
  }

  private SolicitudCompra solicitud(Long id) {
    return solicitudes
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada"));
  }

  // Todas las transiciones bloquean primero producto, luego almacen y finalmente documento.
  private SolicitudCompra bloquearSolicitud(Long id) {
    var vista = solicitud(id);
    productos.bloquear(vista.getProducto().getId()).orElseThrow();
    almacenes.bloquear(vista.getAlmacen().getId()).orElseThrow();
    var bloqueada = solicitudes.bloquear(id).orElseThrow();
    em.refresh(bloqueada);
    return bloqueada;
  }

  private OrdenCompra bloquearOrden(Long id) {
    var vista =
        ordenes.findById(id).orElseThrow(() -> new NoSuchElementException("Orden no encontrada"));
    bloquearSolicitud(vista.getSolicitud().getId());
    var bloqueada = ordenes.bloquear(id).orElseThrow();
    em.refresh(bloqueada);
    return bloqueada;
  }

  @Transactional
  @PreAuthorize("hasRole('COMPRAS')")
  public Solicitud crear(CrearSolicitud r) {
    if (r.cantidad() == null
        || r.cantidad() <= 0
        || r.montoEstimado() == null
        || r.montoEstimado().signum() <= 0)
      throw new IllegalArgumentException("Cantidad y monto deben ser positivos");
    if (r.claveIdempotencia() == null || r.claveIdempotencia().isBlank())
      throw new IllegalArgumentException("Clave requerida");
    if (r.urgente() && (r.motivoUrgencia() == null || r.motivoUrgencia().isBlank()))
      throw new IllegalArgumentException("La urgencia requiere motivo");
    var usuario = actor.usuario();
    // Serializa claves del mismo solicitante, incluso cuando la solicitud aun no existe.
    usuarios.bloquear(usuario.getId()).orElseThrow();
    var previa =
        solicitudes.findByUsuarioIdAndClaveIdempotencia(usuario.getId(), r.claveIdempotencia());
    if (previa.isPresent()) {
      var s = previa.get();
      if (!s.getProducto().getId().equals(r.productoId())
          || !s.getAlmacen().getId().equals(r.almacenId())
          || s.getCantidad() != r.cantidad()
          || s.getMontoEstimado().compareTo(r.montoEstimado()) != 0
          || !Objects.equals(s.getMotivo(), r.motivo())
          || s.isUrgente() != r.urgente()
          || !Objects.equals(s.getMotivoUrgencia(), r.motivoUrgencia()))
        throw new ConflictoCatalogoException("La clave ya fue usada con otro contenido");
      return s.getRespuestaCreacion() == null
          ? respuesta(s)
          : json.readValue(s.getRespuestaCreacion(), Solicitud.class);
    }
    var p =
        productos
            .bloquear(r.productoId())
            .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));
    var a =
        almacenes
            .bloquear(r.almacenId())
            .orElseThrow(() -> new NoSuchElementException("Almacen no encontrado"));
    activo(p, a);
    var s =
        new SolicitudCompra(
            usuario, p, a, r.cantidad(), r.motivo(), ahora(), r.claveIdempotencia());
    s.configurarCompra(r.montoEstimado(), r.urgente(), r.motivoUrgencia());
    solicitudes.saveAndFlush(s);
    var respuesta = respuesta(s);
    s.guardarRespuestaCreacion(json.writeValueAsString(respuesta));
    LOG.info("solicitud_creada id={} actor={}", s.getId(), usuario.getId());
    return respuesta;
  }

  @Transactional
  @PreAuthorize("hasRole('SUPERVISOR')")
  public Solicitud aprobar(Long id, Aprobar r) {
    var s = bloquearSolicitud(id);
    pendiente(s);
    activo(s.getProducto(), s.getAlmacen());
    if (s.getMontoEstimado().signum() <= 0)
      throw new ConflictoCatalogoException("Solicitud historica sin monto valido");
    if (s.getMontoEstimado().compareTo(umbral) > 0
        && (r.justificacion() == null || r.justificacion().isBlank()))
      throw new IllegalArgumentException(
          "El monto supera el umbral configurado; requiere justificacion");
    if (s.getUsuario().getId().equals(actor.usuario().getId()))
      throw new ConflictoCatalogoException("No puede aprobar su propia solicitud");
    s.justificar(r.justificacion());
    s.aprobar(actor.usuario(), ahora());
    LOG.info("solicitud_aprobada id={} actor={}", id, actor.usuario().getId());
    return respuesta(s);
  }

  private void pendiente(SolicitudCompra s) {
    if (s.getEstado() != EstadoSolicitudCompra.PENDIENTE)
      throw new ConflictoCatalogoException("Solicitud ya decidida");
  }

  @Transactional
  @PreAuthorize("hasRole('SUPERVISOR')")
  public Solicitud rechazar(Long id, Motivo r) {
    var s = bloquearSolicitud(id);
    pendiente(s);
    s.rechazar(actor.usuario(), r.motivo(), ahora());
    return respuesta(s);
  }

  @Transactional
  @PreAuthorize("hasRole('SUPERVISOR')")
  public Solicitud duplicada(Long id, Duplicidad r) {
    var s = bloquearSolicitud(id);
    pendiente(s);
    var original = solicitud(r.originalId());
    if (original.getId() >= s.getId()
        || original.getDuplicadaDe() != null
        || !original.getProducto().getId().equals(s.getProducto().getId())
        || !original.getAlmacen().getId().equals(s.getAlmacen().getId()))
      throw new ConflictoCatalogoException(
          "El original debe ser anterior, del mismo producto y almacen, y no duplicado");
    s.confirmarDuplicada(original, actor.usuario(), ahora(), r.motivo());
    return respuesta(s);
  }

  @Transactional
  @PreAuthorize("hasRole('COMPRAS')")
  public Orden emitir(Long solicitudId) {
    var s = bloquearSolicitud(solicitudId);
    var previa = ordenes.findBySolicitudId(solicitudId);
    if (previa.isPresent()) return respuesta(previa.get());
    activo(s.getProducto(), s.getAlmacen());
    if (s.getEstado() != EstadoSolicitudCompra.APROBADA || s.getDuplicadaDe() != null)
      throw new ConflictoCatalogoException("Se requiere solicitud aprobada sin duplicidad");
    var o =
        ordenes.saveAndFlush(
            new OrdenCompra(
                s,
                actor.usuario(),
                "OC-" + UUID.randomUUID(),
                s.isUrgente(),
                s.getMotivoUrgencia(),
                ahora(),
                s.getMontoEstimado()));
    LOG.info("orden_emitida id={} solicitud={}", o.getId(), solicitudId);
    return respuesta(o);
  }

  @Transactional
  @PreAuthorize("hasRole('ALMACEN')")
  public Recepcion recibir(Long id, Recibir r) {
    var o = bloquearOrden(id);
    var previa = recepciones.findByOrdenIdAndClaveIdempotencia(id, r.claveIdempotencia());
    if (previa.isPresent()) {
      var x = previa.get();
      if (x.getCantidad() != r.cantidad() || !Objects.equals(x.getMotivo(), r.motivo()))
        throw new ConflictoCatalogoException("Clave de recepcion usada con otro contenido");
      return respuesta(x);
    }
    o.recibir(r.cantidad());
    var s = o.getSolicitud();
    var m =
        inventario.registrar(
            new RegistrarMovimientoRequest(
                s.getProducto().getId(),
                s.getAlmacen().getId(),
                TipoMovimiento.ENTRADA,
                r.cantidad(),
                null,
                r.motivo()));
    var x =
        recepciones.saveAndFlush(
            new RecepcionCompra(
                o,
                actor.usuario(),
                m.id(),
                r.cantidad(),
                r.motivo(),
                m.fecha(),
                r.claveIdempotencia()));
    LOG.info("compra_recibida orden={} movimiento={} cantidad={}", id, m.id(), r.cantidad());
    return respuesta(x);
  }

  @Transactional
  @PreAuthorize("hasRole('SUPERVISOR')")
  public Orden cancelar(Long id, Motivo r) {
    var o = bloquearOrden(id);
    o.cancelar(actor.usuario(), ahora(), r.motivo());
    return respuesta(o);
  }

  public Solicitud obtenerSolicitud(Long id) {
    return respuesta(solicitud(id));
  }

  public Orden obtenerOrden(Long id) {
    return respuesta(
        ordenes.findById(id).orElseThrow(() -> new NoSuchElementException("Orden no encontrada")));
  }

  public Page<Solicitud> listarSolicitudes(
      String sku,
      Long almacen,
      EstadoSolicitudCompra estado,
      Instant desde,
      Instant hasta,
      int pagina,
      int tamanio) {
    Periodo.validar(desde, hasta);
    return solicitudes
        .buscar(
            normal(sku),
            almacen,
            estado,
            desde,
            hasta,
            PageRequest.of(pagina, tamanio, Sort.by("enviadaEn", "id").descending()))
        .map(this::respuesta);
  }

  public Page<Orden> listarOrdenes(
      String sku,
      Long almacen,
      EstadoOrdenCompra estado,
      Instant desde,
      Instant hasta,
      int pagina,
      int tamanio) {
    Periodo.validar(desde, hasta);
    return ordenes
        .buscar(
            normal(sku),
            almacen,
            estado,
            desde,
            hasta,
            PageRequest.of(pagina, tamanio, Sort.by("emitidaEn", "id").descending()))
        .map(this::respuesta);
  }

  public Page<Recepcion> recepciones(Long id, int pagina, int tamanio) {
    obtenerOrden(id);
    return recepciones
        .findByOrdenId(id, PageRequest.of(pagina, tamanio, Sort.by("id")))
        .map(this::respuesta);
  }

  private String normal(String sku) {
    return sku == null ? null : sku.trim().toUpperCase(Locale.ROOT);
  }

  private Long uid(Usuario u) {
    return u == null ? null : u.getId();
  }

  private Solicitud respuesta(SolicitudCompra s) {
    var similares =
        solicitudes.similares(
            s.getProducto().getId(),
            s.getAlmacen().getId(),
            s.getId(),
            s.getEnviadaEn().minus(7, ChronoUnit.DAYS));
    return new Solicitud(
        s.getId(),
        s.getProducto().getId(),
        s.getProducto().getSku(),
        s.getAlmacen().getId(),
        s.getCantidad(),
        s.getMotivo(),
        s.getMontoEstimado(),
        s.getEstado().name(),
        uid(s.getUsuario()),
        s.getEnviadaEn(),
        s.getAprobadaEn(),
        s.getRevisadaEn(),
        uid(s.getRevisadoPor()),
        s.getMotivoRechazo(),
        s.getJustificacionAprobacion(),
        s.isUrgente(),
        s.getMotivoUrgencia(),
        s.getDuplicadaDe() == null ? null : s.getDuplicadaDe().getId(),
        s.getDuplicidadConfirmadaEn(),
        uid(s.getDuplicidadConfirmadaPor()),
        s.getMotivoDuplicidad(),
        similares);
  }

  private Orden respuesta(OrdenCompra o) {
    long pendiente =
        o.getEstado() == EstadoOrdenCompra.EMITIDA
            ? o.getSolicitud().getCantidad() - o.getCantidadRecibida()
            : 0;
    return new Orden(
        o.getId(),
        o.getSolicitud().getId(),
        o.getNumeroOrden(),
        o.getEstado().name(),
        o.isUrgente(),
        o.getMotivoUrgencia(),
        o.getMontoEstimado(),
        o.getEmitidaEn(),
        uid(o.getUsuario()),
        o.getSolicitud().getCantidad(),
        o.getCantidadRecibida(),
        pendiente,
        o.getCanceladaEn(),
        o.getMotivoCancelacion());
  }

  private Recepcion respuesta(RecepcionCompra r) {
    return new Recepcion(
        r.getId(),
        r.getOrden().getId(),
        r.getMovimientoId(),
        r.getCantidad(),
        r.getMotivo(),
        r.getFecha(),
        uid(r.getUsuario()));
  }
}
