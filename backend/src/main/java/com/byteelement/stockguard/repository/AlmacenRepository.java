package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.Almacen;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Almacen e where e.id=:id")
  Optional<Almacen> bloquear(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Almacen e order by e.id")
  List<Almacen> bloquearTodos();

  boolean existsByCodigo(String codigo);

  boolean existsByCodigoAndIdNot(String codigo, Long id);
}
