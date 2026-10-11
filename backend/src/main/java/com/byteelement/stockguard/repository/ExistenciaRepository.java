package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.Existencia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExistenciaRepository extends JpaRepository<Existencia, Long> {
  Optional<Existencia> findByProductoIdAndAlmacenId(Long productoId, Long almacenId);
}
