package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.Conteo;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ConteoRepository extends JpaRepository<Conteo, Long> {
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
