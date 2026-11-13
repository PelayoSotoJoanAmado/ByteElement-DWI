package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
  Optional<OrdenCompra> findBySolicitudId(Long solicitudId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from OrdenCompra o where o.id=:id")
  Optional<OrdenCompra> bloquear(Long id);

  @Query(
      """
select o from OrdenCompra o join fetch o.solicitud s
where (:sku is null or s.producto.sku=:sku) and (:almacenId is null or s.almacen.id=:almacenId)
and (:estado is null or o.estado=:estado) and o.emitidaEn>=:desde and o.emitidaEn<:hasta
""")
  Page<OrdenCompra> buscar(
      String sku,
      Long almacenId,
      EstadoOrdenCompra estado,
      Instant desde,
      Instant hasta,
      Pageable pageable);

  @Query(
      """
      select coalesce(sum(o.solicitud.cantidad-o.cantidadRecibida),0) from OrdenCompra o
      where o.solicitud.producto.id=:productoId and o.solicitud.almacen.id=:almacenId
      and o.estado=com.byteelement.stockguard.entity.EstadoOrdenCompra.EMITIDA
      """)
  long pendientes(Long productoId, Long almacenId);

  @Query(
      """
      select count(o),coalesce(sum(case when o.urgente=true then 1 else 0 end),0) from OrdenCompra o
      where o.emitidaEn>=:desde and o.emitidaEn<:hasta
      and (:sku is null or o.solicitud.producto.sku=:sku)
      and (:almacenId is null or o.solicitud.almacen.id=:almacenId)
      """)
  List<Object[]> urgencias(Instant desde, Instant hasta, String sku, Long almacenId);
}
