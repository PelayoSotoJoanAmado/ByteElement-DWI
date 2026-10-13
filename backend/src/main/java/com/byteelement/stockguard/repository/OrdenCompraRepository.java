package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.OrdenCompra;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {
  Optional<OrdenCompra> findByNumeroOrden(String numeroOrden);
  Optional<OrdenCompra> findBySolicitudId(Long solicitudId);
}

