package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.PoliticaStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PoliticaStockRepository extends JpaRepository<PoliticaStock, Long> {
    Optional<PoliticaStock> findByProductoIdAndAlmacenId(Long productoId, Long almacenId);
}
