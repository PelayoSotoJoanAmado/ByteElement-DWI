package com.byteelement.stockguard.service;

import com.byteelement.stockguard.entity.Producto;
import com.byteelement.stockguard.exception.SkuDuplicadoException;
import com.byteelement.stockguard.dto.CrearProductoRequest;
import com.byteelement.stockguard.repository.ProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Producto crear(CrearProductoRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);

        if (repository.existsBySku(sku)) {
            throw new SkuDuplicadoException(sku);
        }

        Producto producto = new Producto(
                sku,
                request.categoria().trim(),
                request.marca().trim(),
                request.modelo().trim(),
                request.especificacion().trim(),
                request.critico()
        );

        return repository.saveAndFlush(producto);
    }

    public Page<Producto> listar(int pagina, int tamanio) {
        return repository.findAll(
                PageRequest.of(pagina, tamanio, Sort.by("id").ascending())
        );
    }

    public Producto obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Producto no encontrado"));
    }
}