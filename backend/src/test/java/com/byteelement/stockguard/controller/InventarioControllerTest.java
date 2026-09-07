package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.dto.AlmacenResponse;
import com.byteelement.stockguard.dto.ExistenciaResponse;
import com.byteelement.stockguard.dto.RegistrarMovimientoRequest;
import com.byteelement.stockguard.entity.Movimiento;
import com.byteelement.stockguard.entity.TipoMovimiento;
import com.byteelement.stockguard.exception.StockInsuficienteException;
import com.byteelement.stockguard.service.InventarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventarioControllerTest {
    @Test
    void consultaConIdentificadorNegativoDevuelve400SinLlamarServicio() throws Exception {
        mockMvc.perform(get("/api/v1/existencias")
                        .param("productoId", "-1").param("almacenId", "1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(inventarioService);
    }

    private InventarioService inventarioService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        inventarioService = mock(InventarioService.class);
        InventarioController controller = new InventarioController(inventarioService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listarAlmacenesRetorna200YLista() throws Exception {
        when(inventarioService.almacenes()).thenReturn(List.of(
                new AlmacenResponse(1L, "A01", "Tienda principal"),
                new AlmacenResponse(2L, "A02", "Depósito"),
                new AlmacenResponse(3L, "A03", "Despacho")
        ));

        mockMvc.perform(get("/api/v1/almacenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("A01"))
                .andExpect(jsonPath("$[1].codigo").value("A02"))
                .andExpect(jsonPath("$[2].codigo").value("A03"));
    }

    @Test
    void registrarMovimientoValidoRetorna201YLocation() throws Exception {
        Movimiento mov = new Movimiento(1L, 1L, 1L, TipoMovimiento.ENTRADA, 10L, "Operador", Instant.now(), "Compra inicial", 10L);
        when(inventarioService.registrar(any(RegistrarMovimientoRequest.class))).thenReturn(mov);

        String json = """
                {
                    "productoId": 1,
                    "almacenId": 1,
                    "tipo": "ENTRADA",
                    "cantidad": 10,
                    "autor": "Operador",
                    "motivo": "Compra inicial"
                }
                """;

        mockMvc.perform(post("/api/v1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/movimientos/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.saldoResultante").value(10));
    }

    @Test
    void registrarMovimientoConStockInsuficienteRetorna409Conflict() throws Exception {
        when(inventarioService.registrar(any(RegistrarMovimientoRequest.class)))
                .thenThrow(new StockInsuficienteException());

        String json = """
                {
                    "productoId": 1,
                    "almacenId": 1,
                    "tipo": "SALIDA",
                    "cantidad": 50,
                    "autor": "Operador",
                    "motivo": "Venta excesiva"
                }
                """;

        mockMvc.perform(post("/api/v1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Stock insuficiente para registrar la salida."));
    }

    @Test
    void registrarMovimientoConCamposInvalidosRetorna400BadRequest() throws Exception {
        String json = """
                {
                    "productoId": -1,
                    "almacenId": null,
                    "tipo": null,
                    "cantidad": 0,
                    "autor": "",
                    "motivo": ""
                }
                """;

        mockMvc.perform(post("/api/v1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerMovimientoExistenteRetorna200() throws Exception {
        Movimiento mov = new Movimiento(1L, 1L, 1L, TipoMovimiento.ENTRADA, 10L, "Operador", Instant.now(), "Entrada", 10L);
        when(inventarioService.obtenerMovimiento(1L)).thenReturn(mov);

        mockMvc.perform(get("/api/v1/movimientos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cantidad").value(10));
    }

    @Test
    void obtenerMovimientoInexistenteRetorna404() throws Exception {
        when(inventarioService.obtenerMovimiento(99L)).thenThrow(new NoSuchElementException("Movimiento no encontrado con ID: 99"));

        mockMvc.perform(get("/api/v1/movimientos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarExistenciaRetorna200() throws Exception {
        when(inventarioService.consultar(1L, 1L)).thenReturn(new ExistenciaResponse(1L, 1L, 15L));

        mockMvc.perform(get("/api/v1/existencias")
                        .param("productoId", "1")
                        .param("almacenId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.almacenId").value(1))
                .andExpect(jsonPath("$.cantidad").value(15));
    }

    @Test
    void obtenerAlmacenPorIdRetorna200() throws Exception {
        when(inventarioService.obtenerAlmacen(1L)).thenReturn(new AlmacenResponse(1L, "A01", "Tienda principal"));

        mockMvc.perform(get("/api/v1/almacenes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigo").value("A01"));
    }

    @Test
    void listarMovimientosRetorna200() throws Exception {
        Movimiento mov = new Movimiento(1L, 1L, 1L, TipoMovimiento.ENTRADA, 10L, "Operador", Instant.now(), "Entrada", 10L);
        when(inventarioService.listarMovimientos(1L, null)).thenReturn(List.of(mov));

        mockMvc.perform(get("/api/v1/movimientos")
                        .param("productoId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void transferirValidoRetorna201() throws Exception {
        var resp = new com.byteelement.stockguard.dto.TransferenciaResponse(
                1L, 2L, 1L, 1L, 2L, 4L, 6L, 4L, "Operador", "Reabastecimiento", Instant.now()
        );
        when(inventarioService.transferir(any(com.byteelement.stockguard.dto.RegistrarTransferenciaRequest.class)))
                .thenReturn(resp);

        String json = """
                {
                    "productoId": 1,
                    "origenAlmacenId": 1,
                    "destinoAlmacenId": 2,
                    "cantidad": 4,
                    "autor": "Operador",
                    "motivo": "Reabastecimiento"
                }
                """;

        mockMvc.perform(post("/api/v1/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movimientoSalidaId").value(1))
                .andExpect(jsonPath("$.movimientoEntradaId").value(2))
                .andExpect(jsonPath("$.saldoOrigenResultante").value(6))
                .andExpect(jsonPath("$.saldoDestinoResultante").value(4));
    }

    @Test
    void transferirConStockInsuficienteRetorna409Conflict() throws Exception {
        when(inventarioService.transferir(any(com.byteelement.stockguard.dto.RegistrarTransferenciaRequest.class)))
                .thenThrow(new StockInsuficienteException());

        String json = """
                {
                    "productoId": 1,
                    "origenAlmacenId": 1,
                    "destinoAlmacenId": 2,
                    "cantidad": 50,
                    "autor": "Operador",
                    "motivo": "Transferencia excesiva"
                }
                """;

        mockMvc.perform(post("/api/v1/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void transferirMismoAlmacenRetorna400BadRequest() throws Exception {
        when(inventarioService.transferir(any(com.byteelement.stockguard.dto.RegistrarTransferenciaRequest.class)))
                .thenThrow(new IllegalArgumentException("El almacén de origen y destino no pueden ser el mismo."));

        String json = """
                {
                    "productoId": 1,
                    "origenAlmacenId": 1,
                    "destinoAlmacenId": 1,
                    "cantidad": 5,
                    "autor": "Operador",
                    "motivo": "Transferencia invalida"
                }
                """;

        mockMvc.perform(post("/api/v1/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El almacén de origen y destino no pueden ser el mismo."));
    }
}
