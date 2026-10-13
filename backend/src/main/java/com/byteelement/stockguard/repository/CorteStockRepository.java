package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.CorteStock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface CorteStockRepository extends JpaRepository<CorteStock, Long> {
  @Query("select c from CorteStock c where c.fecha<:hasta order by c.fecha desc,c.id desc")
  List<CorteStock> ultimo(Instant hasta, Pageable pagina);

  @Query(
      "select c from CorteStock c join fetch c.producto join fetch c.almacen where c.lote=:lote"
          + " order by c.id")
  List<CorteStock> lote(String lote);
}
