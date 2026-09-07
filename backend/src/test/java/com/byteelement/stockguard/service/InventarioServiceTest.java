package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.RegistrarMovimientoRequest;
import com.byteelement.stockguard.entity.Producto;
import com.byteelement.stockguard.entity.TipoMovimiento;
import com.byteelement.stockguard.exception.StockInsuficienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventarioServiceTest {
    @Test
    void productoDesactivadoConservaSaldoEHistorialPeroRechazaNuevosMovimientos() {
        service.registrar(movimiento(TipoMovimiento.ENTRADA, 5, 1));
        productos.obtener(1L).desactivar();
        assertThrows(com.byteelement.stockguard.exception.ConflictoCatalogoException.class,
                () -> service.registrar(movimiento(TipoMovimiento.SALIDA, 1, 1)));
        assertEquals(5, service.consultar(1, 1).cantidad());
        assertEquals(1, service.listarMovimientos(1L, null).size());
    }

    @Test
    void almacenDesactivadoConservaSaldoPeroRechazaTransferencias() {
        service.registrar(movimiento(TipoMovimiento.ENTRADA, 5, 1));
        var almacen = almacenes.obtener(1L);
        almacen.desactivar();
        when(almacenes.obtener(1L)).thenReturn(almacen);
        assertThrows(com.byteelement.stockguard.exception.ConflictoCatalogoException.class, () ->
                service.transferir(new com.byteelement.stockguard.dto.RegistrarTransferenciaRequest(
                        1L, 1L, 2L, 1L, "Operador", "Prueba")));
        assertEquals(5, service.consultar(1, 1).cantidad());
        assertEquals(1, service.listarMovimientos(1L, null).size());
    }
    @Test
    void consultaProductoInexistenteNoInventaSaldoCero() {
        when(productos.obtener(99L)).thenThrow(new NoSuchElementException("Producto no encontrado"));
        assertThrows(NoSuchElementException.class, () -> service.consultar(99, 1));
    }

    @Test
    void entradaQueDesbordaSaldoSeRechazaSinModificarInventario() {
        service.registrar(movimiento(TipoMovimiento.ENTRADA, Long.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(movimiento(TipoMovimiento.ENTRADA, 1, 1)));
        assertEquals(Long.MAX_VALUE, service.consultar(1, 1).cantidad());
        assertEquals(1, service.listarMovimientos(1L, null).size());
    }

    @Test
    void transferenciaQueDesbordaDestinoNoModificaNingunSaldo() {
        service.registrar(movimiento(TipoMovimiento.ENTRADA, 1, 1));
        service.registrar(movimiento(TipoMovimiento.ENTRADA, Long.MAX_VALUE, 2));
        var request = new com.byteelement.stockguard.dto.RegistrarTransferenciaRequest(
                1L, 1L, 2L, 1L, "Operador", "Prueba de limite numerico");
        assertThrows(IllegalArgumentException.class, () -> service.transferir(request));
        assertEquals(1, service.consultar(1, 1).cantidad());
        assertEquals(Long.MAX_VALUE, service.consultar(1, 2).cantidad());
        assertEquals(2, service.listarMovimientos(1L, null).size());
    }
    private InventarioService service;
    private ProductoService productos;
    private AlmacenService almacenes;

    @BeforeEach
    void preparar() {
        productos = mock(ProductoService.class);
        when(productos.obtener(1L)).thenReturn(new Producto(
                "RAM-01", "RAM", "Kingston", "DDR4", "8 GB", true));
        almacenes = mock(AlmacenService.class);
        when(almacenes.obtener(anyLong())).thenAnswer(invocacion -> {
            long id = invocacion.getArgument(0);
            if (id < 1 || id > 3) throw new NoSuchElementException("Almacen no encontrado");
            var almacen = new com.byteelement.stockguard.entity.Almacen("A0" + id,
                    id == 1 ? "Tienda principal" : "Almacen de prueba");
            org.springframework.test.util.ReflectionTestUtils.setField(almacen, "id", id);
            return almacen;
        });
        service = new InventarioService(productos, almacenes);
    }

    private RegistrarMovimientoRequest movimiento(TipoMovimiento tipo, long cantidad, long almacen) {
        return new RegistrarMovimientoRequest(1L, almacen, tipo, cantidad,
                "Operador de prueba", "Demostracion APF1");
    }

    @Test
    void entradaIncrementaSaldoYConservaDatosDelMovimiento() {
        var movimiento = service.registrar(movimiento(TipoMovimiento.ENTRADA, 10, 1));
        assertEquals(10, service.consultar(1, 1).cantidad());
        assertEquals(10, movimiento.saldoResultante());
        assertEquals("Operador de prueba", movimiento.autor());
        assertEquals("Demostracion APF1", movimiento.motivo());
        assertNotNull(movimiento.fecha());
        assertEquals(movimiento, service.obtenerMovimiento(movimiento.id()));
    }

    @Test
    void salidaConSaldoSuficienteDescuentaUnidades() {
        service.registrar(movimiento(TipoMovimiento.ENTRADA, 10, 1));
        service.registrar(movimiento(TipoMovimiento.SALIDA, 4, 1));
        assertEquals(6, service.consultar(1, 1).cantidad());
    }

    @Test
    void salidaExcesivaNoModificaSaldoNiRegistraMovimiento() {
        service.registrar(movimiento(TipoMovimiento.ENTRADA, 10, 1));
        assertThrows(StockInsuficienteException.class,
                () -> service.registrar(movimiento(TipoMovimiento.SALIDA, 11, 1)));
        assertEquals(10, service.consultar(1, 1).cantidad());
        assertThrows(NoSuchElementException.class, () -> service.obtenerMovimiento(2));
    }

    @Test
    void salidaExactaPermiteLlegarACero() {
        service.registrar(movimiento(TipoMovimiento.ENTRADA, 10, 1));
        service.registrar(movimiento(TipoMovimiento.SALIDA, 10, 1));
        assertEquals(0, service.consultar(1, 1).cantidad());
    }

    @Test
    void saldoPerteneceAlParProductoAlmacen() {
        service.registrar(movimiento(TipoMovimiento.ENTRADA, 10, 1));
        assertEquals(0, service.consultar(1, 2).cantidad());
        assertThrows(StockInsuficienteException.class,
                () -> service.registrar(movimiento(TipoMovimiento.SALIDA, 1, 2)));
    }

    @Test
    void rechazaAlmacenInexistente() {
        assertThrows(NoSuchElementException.class,
                () -> service.registrar(movimiento(TipoMovimiento.ENTRADA, 1, 99)));
    }

    @Test
    void rechazaProductoInexistenteSinGuardar() {
        when(productos.obtener(1L)).thenThrow(new NoSuchElementException("Producto no encontrado"));
        assertThrows(NoSuchElementException.class,
                () -> service.registrar(movimiento(TipoMovimiento.ENTRADA, 1, 1)));
        assertThrows(NoSuchElementException.class, () -> service.obtenerMovimiento(1));
    }

    @Test
    void transferenciaConSaldoSuficienteMueveUnidadesEntreAlmacenes() {
        service.registrar(movimiento(TipoMovimiento.ENTRADA, 10, 1));
        var req = new com.byteelement.stockguard.dto.RegistrarTransferenciaRequest(
                1L, 1L, 2L, 4L, "Operador", "Reabastecimiento de depósito"
        );
        var resp = service.transferir(req);

        assertEquals(6, service.consultar(1, 1).cantidad());
        assertEquals(4, service.consultar(1, 2).cantidad());
        assertEquals(6, resp.saldoOrigenResultante());
        assertEquals(4, resp.saldoDestinoResultante());
        assertEquals(3, service.listarMovimientos(1L, null).size());
    }

    @Test
    void transferenciaConSaldoInsuficienteRechazaSinAlterarSaldos() {
        service.registrar(movimiento(TipoMovimiento.ENTRADA, 5, 1));
        var req = new com.byteelement.stockguard.dto.RegistrarTransferenciaRequest(
                1L, 1L, 2L, 10L, "Operador", "Intento de sobre-transferencia"
        );
        assertThrows(StockInsuficienteException.class, () -> service.transferir(req));
        assertEquals(5, service.consultar(1, 1).cantidad());
        assertEquals(0, service.consultar(1, 2).cantidad());
    }

    @Test
    void transferenciaMismoAlmacenRechaza() {
        var req = new com.byteelement.stockguard.dto.RegistrarTransferenciaRequest(
                1L, 1L, 1L, 2L, "Operador", "Transferencia inválida"
        );
        assertThrows(IllegalArgumentException.class, () -> service.transferir(req));
    }

    @Test
    void obtenerAlmacenExistenteRetornaDatos() {
        var a = service.obtenerAlmacen(1L);
        assertEquals("A01", a.codigo());
        assertEquals("Tienda principal", a.nombre());
    }

    @Test
    void obtenerAlmacenInexistenteLanzaExcepcion() {
        assertThrows(NoSuchElementException.class, () -> service.obtenerAlmacen(99L));
    }
}
