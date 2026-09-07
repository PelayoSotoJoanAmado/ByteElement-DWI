package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.AlmacenResponse;
import com.byteelement.stockguard.dto.ExistenciaResponse;
import com.byteelement.stockguard.dto.RegistrarMovimientoRequest;
import com.byteelement.stockguard.dto.RegistrarTransferenciaRequest;
import com.byteelement.stockguard.dto.TransferenciaResponse;
import com.byteelement.stockguard.entity.Movimiento;
import com.byteelement.stockguard.entity.TipoMovimiento;
import com.byteelement.stockguard.exception.StockInsuficienteException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Profile({"default", "apf1"})
public class InventarioService {

    private final ProductoService productos;

    private final AlmacenService catalogoAlmacenes;

    private final Map<String, Long> existencias = new ConcurrentHashMap<>();
    private final Map<Long, Movimiento> movimientos = new ConcurrentHashMap<>();
    private final AtomicLong movimientoIdSecuencia = new AtomicLong(1);

    public InventarioService(ProductoService productos, AlmacenService catalogoAlmacenes) {
        this.productos = productos;
        this.catalogoAlmacenes = catalogoAlmacenes;
    }

    public synchronized Movimiento registrar(RegistrarMovimientoRequest request) {
        // 1. Validar producto existente
        validarProductoActivo(request.productoId());

        // 2. Validar almacén existente
        validarAlmacenActivo(request.almacenId());

        // 3. Obtener saldo actual
        String clave = claveExistencia(request.productoId(), request.almacenId());
        long saldoActual = existencias.getOrDefault(clave, 0L);

        // 4. Regla de negocio: impedir saldo negativo
        long nuevoSaldo;
        if (request.tipo() == TipoMovimiento.ENTRADA) {
            nuevoSaldo = sumarSaldo(saldoActual, request.cantidad());
        } else if (request.tipo() == TipoMovimiento.SALIDA) {
            if (saldoActual < request.cantidad()) {
                throw new StockInsuficienteException();
            }
            nuevoSaldo = saldoActual - request.cantidad();
        } else {
            throw new IllegalArgumentException("Tipo de movimiento no soportado: " + request.tipo());
        }

        // 5. Registrar movimiento y actualizar saldo
        long id = movimientoIdSecuencia.getAndIncrement();
        Movimiento movimiento = new Movimiento(
                id,
                request.productoId(),
                request.almacenId(),
                request.tipo(),
                request.cantidad(),
                request.autor(),
                Instant.now(),
                request.motivo(),
                nuevoSaldo
        );

        existencias.put(clave, nuevoSaldo);
        movimientos.put(id, movimiento);

        return movimiento;
    }

    public synchronized TransferenciaResponse transferir(RegistrarTransferenciaRequest request) {
        // 1. Validar producto existente
        validarProductoActivo(request.productoId());

        // 2. Validar que almacenes no sean idénticos
        if (request.origenAlmacenId().equals(request.destinoAlmacenId())) {
            throw new IllegalArgumentException("El almacén de origen y destino no pueden ser el mismo.");
        }

        // 3. Validar existencia de ambos almacenes
        validarAlmacenActivo(request.origenAlmacenId());
        validarAlmacenActivo(request.destinoAlmacenId());

        // 4. Regla crítica: validar saldo en origen antes de realizar mutaciones
        String claveOrigen = claveExistencia(request.productoId(), request.origenAlmacenId());
        long saldoOrigen = existencias.getOrDefault(claveOrigen, 0L);
        if (saldoOrigen < request.cantidad()) {
            throw new StockInsuficienteException();
        }

        String claveDestino = claveExistencia(request.productoId(), request.destinoAlmacenId());
        long saldoDestino = existencias.getOrDefault(claveDestino, 0L);

        // 5. Ejecución atómica de ambos efectos
        long nuevoSaldoOrigen = saldoOrigen - request.cantidad();
        long nuevoSaldoDestino = sumarSaldo(saldoDestino, request.cantidad());

        Instant fecha = Instant.now();

        long salidaId = movimientoIdSecuencia.getAndIncrement();
        Movimiento movSalida = new Movimiento(
                salidaId,
                request.productoId(),
                request.origenAlmacenId(),
                TipoMovimiento.TRANSFERENCIA_SALIDA,
                request.cantidad(),
                request.autor(),
                fecha,
                "Transferencia hacia almacén " + request.destinoAlmacenId() + ": " + request.motivo(),
                nuevoSaldoOrigen
        );

        long entradaId = movimientoIdSecuencia.getAndIncrement();
        Movimiento movEntrada = new Movimiento(
                entradaId,
                request.productoId(),
                request.destinoAlmacenId(),
                TipoMovimiento.TRANSFERENCIA_ENTRADA,
                request.cantidad(),
                request.autor(),
                fecha,
                "Transferencia desde almacén " + request.origenAlmacenId() + ": " + request.motivo(),
                nuevoSaldoDestino
        );

        existencias.put(claveOrigen, nuevoSaldoOrigen);
        existencias.put(claveDestino, nuevoSaldoDestino);
        movimientos.put(salidaId, movSalida);
        movimientos.put(entradaId, movEntrada);

        return new TransferenciaResponse(
                salidaId,
                entradaId,
                request.productoId(),
                request.origenAlmacenId(),
                request.destinoAlmacenId(),
                request.cantidad(),
                nuevoSaldoOrigen,
                nuevoSaldoDestino,
                request.autor(),
                request.motivo(),
                fecha
        );
    }

    public synchronized ExistenciaResponse consultar(long productoId, long almacenId) {
        productos.obtener(productoId);
        catalogoAlmacenes.obtener(almacenId);
        String clave = claveExistencia(productoId, almacenId);
        long cantidad = existencias.getOrDefault(clave, 0L);
        return new ExistenciaResponse(productoId, almacenId, cantidad);
    }

    public List<AlmacenResponse> almacenes() { return catalogoAlmacenes.listar().stream().map(AlmacenResponse::desde).toList(); }

    public AlmacenResponse obtenerAlmacen(long id) { return AlmacenResponse.desde(catalogoAlmacenes.obtener(id)); }

    public synchronized Movimiento obtenerMovimiento(long id) {
        Movimiento movimiento = movimientos.get(id);
        if (movimiento == null) {
            throw new NoSuchElementException("Movimiento no encontrado con ID: " + id);
        }
        return movimiento;
    }

    public synchronized List<Movimiento> listarMovimientos(Long productoId, Long almacenId) {
        return movimientos.values().stream()
                .filter(m -> productoId == null || m.productoId() == productoId)
                .filter(m -> almacenId == null || m.almacenId() == almacenId)
                .sorted(Comparator.comparingLong(Movimiento::id))
                .toList();
    }

    private void validarProductoActivo(Long id) {
        if (!productos.obtener(id).isActivo()) {
            throw new com.byteelement.stockguard.exception.ConflictoCatalogoException("El producto esta desactivado.");
        }
    }

    private void validarAlmacenActivo(Long id) {
        if (!catalogoAlmacenes.obtener(id).isActivo()) {
            throw new com.byteelement.stockguard.exception.ConflictoCatalogoException("El almacen esta desactivado.");
        }
    }
    private long sumarSaldo(long saldo, long cantidad) {
        try {
            return Math.addExact(saldo, cantidad);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("La cantidad supera la capacidad numerica del saldo.");
        }
    }

    private String claveExistencia(long productoId, long almacenId) {
        return productoId + ":" + almacenId;
    }
}
