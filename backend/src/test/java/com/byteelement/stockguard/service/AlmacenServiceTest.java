package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.GuardarAlmacenRequest;
import com.byteelement.stockguard.entity.Almacen;
import com.byteelement.stockguard.exception.ConflictoCatalogoException;
import com.byteelement.stockguard.repository.AlmacenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlmacenServiceTest {
    AlmacenRepository repository;
    AlmacenService service;
    @BeforeEach void preparar() {
        repository = mock(AlmacenRepository.class);
        service = new AlmacenService(repository);
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void crearNormalizaCodigoYGuardaActivo() {
        var result = service.crear(new GuardarAlmacenRequest(" a04 ", " Nuevo almacen "));
        assertEquals("A04", result.getCodigo());
        assertEquals("Nuevo almacen", result.getNombre());
        assertTrue(result.isActivo());
    }
    @Test void rechazaCodigoDuplicadoSinGuardar() {
        when(repository.existsByCodigo("A01")).thenReturn(true);
        assertThrows(ConflictoCatalogoException.class, () -> service.crear(new GuardarAlmacenRequest("a01", "Tienda")));
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void editaNombreConservandoCodigoPropio() {
        var almacen = new Almacen("A01", "Anterior");
        when(repository.findById(1L)).thenReturn(Optional.of(almacen));
        service.actualizar(1L, new GuardarAlmacenRequest("A01", "Nuevo"));
        assertEquals("Nuevo", almacen.getNombre());
        verify(repository).existsByCodigoAndIdNot("A01", 1L);
    }
    @Test void rechazaCodigoDeOtroAlmacenSinCambiarElOriginal() {
        var almacen = new Almacen("A01", "Anterior");
        when(repository.findById(1L)).thenReturn(Optional.of(almacen));
        when(repository.existsByCodigoAndIdNot("A02", 1L)).thenReturn(true);
        assertThrows(ConflictoCatalogoException.class, () ->
                service.actualizar(1L, new GuardarAlmacenRequest("A02", "Nuevo")));
        assertEquals("A01", almacen.getCodigo());
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void desactivarConservaRegistro() {
        var almacen = new Almacen("A01", "Tienda");
        when(repository.findById(1L)).thenReturn(Optional.of(almacen));
        service.desactivar(1L);
        assertFalse(almacen.isActivo());
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }
}
