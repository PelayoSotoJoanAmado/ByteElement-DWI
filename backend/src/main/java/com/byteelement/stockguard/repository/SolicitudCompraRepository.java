package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface SolicitudCompraRepository extends JpaRepository<SolicitudCompra, Long> {
  Optional<SolicitudCompra> findByUsuarioIdAndClaveIdempotencia(Long usuarioId, String clave);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SolicitudCompra s where s.id=:id")
  Optional<SolicitudCompra> bloquear(Long id);

  @Query(
      """
      select s.id from SolicitudCompra s where s.producto.id=:productoId and s.almacen.id=:almacenId
      and s.id<>:excluir and s.enviadaEn>=:desde and s.duplicadaDe is null and
      (s.estado=com.byteelement.stockguard.entity.EstadoSolicitudCompra.PENDIENTE or
      (s.estado=com.byteelement.stockguard.entity.EstadoSolicitudCompra.APROBADA and
      not exists(select o.id from OrdenCompra o where o.solicitud=s))) order by s.id
      """)
  List<Long> similares(Long productoId, Long almacenId, Long excluir, Instant desde);

  @Query(
      """
      select s from SolicitudCompra s join fetch s.producto p join fetch s.almacen a
      where (:sku is null or p.sku=:sku) and (:almacenId is null or a.id=:almacenId)
      and (:estado is null or s.estado=:estado) and s.enviadaEn>=:desde and s.enviadaEn<:hasta
      """)
  Page<SolicitudCompra> buscar(
      String sku,
      Long almacenId,
      EstadoSolicitudCompra estado,
      Instant desde,
      Instant hasta,
      Pageable pageable);

  @Query(
      """
select count(s), coalesce(sum(case when s.duplicidadConfirmadaEn is not null then 1 else 0 end),0)
from SolicitudCompra s where s.enviadaEn>=:desde and s.enviadaEn<:hasta
and (:sku is null or s.producto.sku=:sku) and (:almacenId is null or s.almacen.id=:almacenId)
""")
  List<Object[]> duplicidades(Instant desde, Instant hasta, String sku, Long almacenId);

  @Query(
      """
      select s.enviadaEn,s.aprobadaEn from SolicitudCompra s
      where s.estado=com.byteelement.stockguard.entity.EstadoSolicitudCompra.APROBADA
      and s.aprobadaEn>=:desde and s.aprobadaEn<:hasta
      and (:sku is null or s.producto.sku=:sku) and (:almacenId is null or s.almacen.id=:almacenId)
      """)
  List<Object[]> tiempos(Instant desde, Instant hasta, String sku, Long almacenId);
}
