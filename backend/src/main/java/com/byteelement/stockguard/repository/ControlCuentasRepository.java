package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.ControlCuentas;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface ControlCuentasRepository extends JpaRepository<ControlCuentas, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from ControlCuentas c where c.id=1")
  ControlCuentas bloquear();
}
