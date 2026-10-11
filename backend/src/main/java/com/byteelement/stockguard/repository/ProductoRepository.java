package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.Producto;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Producto e where e.id=:id")
  Optional<Producto> bloquear(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Producto e order by e.id")
  List<Producto> bloquearTodos();

  boolean existsBySku(String sku);

  boolean existsBySkuAndIdNot(String sku, Long id);
}
