package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.GuardarPoliticaStockRequest;
import com.byteelement.stockguard.dto.PoliticaStockResponse;
import com.byteelement.stockguard.repository.PoliticaStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class PoliticaStockService {
    private final PoliticaStockRepository repository;
    private final ProductoService productos;
    private final AlmacenService almacenes;

    public PoliticaStockService(PoliticaStockRepository repository, ProductoService productos, AlmacenService almacenes) {
        this.repository = repository; this.productos = productos; this.almacenes = almacenes;
    }
    public PoliticaStockResponse obtener(Long productoId, Long almacenId) {
        return PoliticaStockResponse.desde(repository.findByProductoIdAndAlmacenId(productoId, almacenId)
                .orElseThrow(() -> new NoSuchElementException("Politica de stock no encontrada")));
    }
    @Transactional
    public PoliticaStockResponse guardar(Long productoId, Long almacenId, GuardarPoliticaStockRequest request) {
        if (request.minimo() == null || request.maximo() == null
                || request.minimo() < 0 || request.maximo() <= request.minimo()) {
            throw new IllegalArgumentException("El minimo debe ser >= 0 y el maximo debe ser mayor que el minimo.");
        }
        
        var producto = productos.obtener(productoId);
        var almacen = almacenes.obtener(almacenId);
        if (!producto.isActivo() || !almacen.isActivo()) {
            throw new com.byteelement.stockguard.exception.ConflictoCatalogoException(
                    "La politica requiere un producto y un almacen activos.");
        }
        var politica = repository.findByProductoIdAndAlmacenId(productoId, almacenId)
                .orElseGet(() -> new com.byteelement.stockguard.entity.PoliticaStock(
                        producto, almacen, request.minimo(), request.maximo()));
        politica.actualizar(request.minimo(), request.maximo());
        return PoliticaStockResponse.desde(repository.saveAndFlush(politica));
    }
}
