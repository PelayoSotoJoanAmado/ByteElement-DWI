package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.Conteo;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ConteoRepository extends JpaRepository<Conteo, Long> {
  @Query(
      "select c from Conteo c join fetch c.producto p join fetch c.almacen a where (:sku is null or"
          + " p.sku=:sku) and (:almacenId is null or a.id=:almacenId) and (:confirmado is null or"
          + " c.confirmado=:confirmado) and c.fechaCorte>=:desde and c.fechaCorte<:hasta")
  org.springframework.data.domain.Page<Conteo> buscar(
      String sku,
      Long almacenId,
      Boolean confirmado,
      Instant desde,
      Instant hasta,
      org.springframework.data.domain.Pageable pageable);

  @Query("select c.producto.id,c.almacen.id from Conteo c where c.id=:id")
  List<Object[]> ids(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Conteo c where c.id=:id")
  Optional<Conteo> bloquear(Long id);

  @Query(
      "select c from Conteo c join fetch c.producto join fetch c.almacen where c.fechaCorte>=:desde"
          + " and c.fechaCorte<:hasta")
  List<Conteo> periodo(Instant desde, Instant hasta);
}
