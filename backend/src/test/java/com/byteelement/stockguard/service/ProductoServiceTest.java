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
    @Test
    void editaElMismoSkuSinConfundirloConOtroProducto() {
        var producto = new Producto("ANTERIOR", "RAM", "Marca", "Modelo", "Detalle", true);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(producto));
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var resultado = service.actualizar(1L, solicitud());
        assertEquals("RAM-DDR4-8GB", resultado.getSku());
        verify(repository).existsBySkuAndIdNot("RAM-DDR4-8GB", 1L);
    }

    @Test
    void edicionRechazaSkuDeOtroProductoSinModificarElOriginal() {
        var producto = new Producto("ORIGINAL", "RAM", "Marca", "Modelo", "Detalle", true);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(producto));
        when(repository.existsBySkuAndIdNot("RAM-DDR4-8GB", 1L)).thenReturn(true);
        assertThrows(SkuDuplicadoException.class, () -> service.actualizar(1L, solicitud()));
        assertEquals("ORIGINAL", producto.getSku());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void desactivarConservaElProductoYEsRepetible() {
        var producto = new Producto("P01", "RAM", "Marca", "Modelo", "Detalle", true);
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(producto));
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        service.desactivar(1L);
        service.desactivar(1L);
        assertFalse(producto.isActivo());
        assertEquals("P01", producto.getSku());
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

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
