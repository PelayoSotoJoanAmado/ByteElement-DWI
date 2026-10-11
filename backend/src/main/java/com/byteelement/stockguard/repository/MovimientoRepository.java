package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.MovimientoPersistido;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface MovimientoRepository extends JpaRepository<MovimientoPersistido, Long> {
  @Query(
      "select coalesce(max(m.id),0) from MovimientoPersistido m where m.producto.id=:productoId and"
          + " m.almacen.id=:almacenId")
  long ultimoId(Long productoId, Long almacenId);

  @Query(
      """
       select m from MovimientoPersistido m join fetch m.producto p join fetch m.almacen a
       where (:productoId is null or p.id=:productoId) and (:sku is null or p.sku=:sku)
       and (:almacenId is null or a.id=:almacenId) and m.fecha>=:desde and m.fecha<:hasta
      """)
  Page<MovimientoPersistido> buscar(
      Long productoId, String sku, Long almacenId, Instant desde, Instant hasta, Pageable pageable);

  @Query(
      """
       select m from MovimientoPersistido m join fetch m.producto p join fetch m.almacen a
       where m.fecha>=:desde and m.fecha<:hasta order by m.fecha,m.id
      """)
  List<MovimientoPersistido> periodo(Instant desde, Instant hasta);

  @Query(
      """
       select m from MovimientoPersistido m join fetch m.producto p join fetch m.almacen a
       where p.id=:productoId and a.id=:almacenId and m.fecha<:hasta order by m.fecha desc,m.id desc
      """)
  List<MovimientoPersistido> ultimo(
      Long productoId, Long almacenId, Instant hasta, Pageable pageable);
}
