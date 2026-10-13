package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.EstadoSolicitudCompra;
import com.byteelement.stockguard.entity.SolicitudCompra;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudCompraRepository extends JpaRepository<SolicitudCompra, Long> {
  List<SolicitudCompra> findByEstado(EstadoSolicitudCompra estado);
  Optional<SolicitudCompra> findByClaveIdempotencia(String claveIdempotencia);
  List<SolicitudCompra> findByProductoIdAndAlmacenIdAndEstado(Long productoId, Long almacenId, EstadoSolicitudCompra estado);
}

