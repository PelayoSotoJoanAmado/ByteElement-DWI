package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.RecepcionCompra;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecepcionCompraRepository extends JpaRepository<RecepcionCompra, Long> {
  Optional<RecepcionCompra> findByOrdenIdAndClaveIdempotencia(Long ordenId, String clave);

  Page<RecepcionCompra> findByOrdenId(Long ordenId, Pageable pageable);
}
