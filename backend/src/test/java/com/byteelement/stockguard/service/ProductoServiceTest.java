package com.byteelement.stockguard.service;

import com.byteelement.stockguard.entity.Producto;
import com.byteelement.stockguard.exception.SkuDuplicadoException;
import com.byteelement.stockguard.dto.CrearProductoRequest;
import com.byteelement.stockguard.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductoServiceTest {

    private ProductoRepository repository;
    private ProductoService service;

    @BeforeEach
    void preparar() {
        repository = mock(ProductoRepository.class);
        service = new ProductoService(repository);
    }

    private CrearProductoRequest solicitud() {
        return new CrearProductoRequest(
                " ram-ddr4-8gb ",
                "Memorias RAM",
                "Kingston",
                "DDR4 8 GB",
                "DDR4, 8 GB, 3200 MHz",
                true
        );
    }

    @Test
    void registraProductoConSkuNormalizado() {
        when(repository.existsBySku("RAM-DDR4-8GB")).thenReturn(false);
        when(repository.saveAndFlush(any(Producto.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Producto producto = service.crear(solicitud());

        assertEquals("RAM-DDR4-8GB", producto.getSku());
        assertTrue(producto.isActivo());
        assertTrue(producto.isCritico());
        verify(repository).saveAndFlush(producto);
    }

    @Test
    void rechazaSkuDuplicadoSinGuardar() {
        when(repository.existsBySku("RAM-DDR4-8GB")).thenReturn(true);

        assertThrows(
                SkuDuplicadoException.class,
                () -> service.crear(solicitud())
        );

        verify(repository, never()).saveAndFlush(any(Producto.class));
    }
}