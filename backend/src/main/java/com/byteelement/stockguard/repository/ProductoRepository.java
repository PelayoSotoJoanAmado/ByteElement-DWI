package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
}
