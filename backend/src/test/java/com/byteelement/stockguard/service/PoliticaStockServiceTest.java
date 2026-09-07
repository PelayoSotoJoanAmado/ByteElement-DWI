package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.GuardarPoliticaStockRequest;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.repository.PoliticaStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class PoliticaStockServiceTest {
    PoliticaStockRepository repository;
    ProductoService productos;
    AlmacenService almacenes;
    PoliticaStockService service;
    Producto producto;
    Almacen almacen;
    @BeforeEach void preparar() {
        repository = mock(PoliticaStockRepository.class);
        productos = mock(ProductoService.class);
        almacenes = mock(AlmacenService.class);
        producto = new Producto("P01", "RAM", "Marca", "Modelo", "Detalle", true);
        almacen = new Almacen("A01", "Tienda");
        when(productos.obtener(1L)).thenReturn(producto);
        when(almacenes.obtener(2L)).thenReturn(almacen);
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        service = new PoliticaStockService(repository, productos, almacenes);
    }
    @Test void aceptaMinimoCeroYMaximoMayor() {
        var resultado = service.guardar(1L, 2L, new GuardarPoliticaStockRequest(0L, 10L));
        assertEquals(0, resultado.minimo());
        assertEquals(10, resultado.maximo());
        verify(repository).saveAndFlush(any(PoliticaStock.class));
    }
    @Test void rechazaMinimoNegativoSinGuardar() {
        assertThrows(IllegalArgumentException.class, () ->
                service.guardar(1L, 2L, new GuardarPoliticaStockRequest(-1L, 10L)));
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void rechazaMaximoIgualOMenorSinGuardar() {
        for (long maximo : new long[]{4, 5}) {
            assertThrows(IllegalArgumentException.class, () ->
                    service.guardar(1L, 2L, new GuardarPoliticaStockRequest(5L, maximo)));
        }
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void actualizarConservaLaMismaPoliticaDelPar() {
        var existente = new PoliticaStock(producto, almacen, 1, 5);
        when(repository.findByProductoIdAndAlmacenId(1L, 2L)).thenReturn(Optional.of(existente));
        service.guardar(1L, 2L, new GuardarPoliticaStockRequest(3L, 20L));
        assertEquals(3, existente.getMinimo());
        verify(repository).saveAndFlush(existente);
    }
}
