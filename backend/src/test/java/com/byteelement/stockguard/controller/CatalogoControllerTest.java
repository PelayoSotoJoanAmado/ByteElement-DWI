package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.service.*;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.NoSuchElementException;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogoControllerTest {
    MockMvc mvc;
    ProductoService productos;
    AlmacenService almacenes;
    PoliticaStockService politicas;
    @BeforeEach void preparar() {
        productos = mock(ProductoService.class);
        almacenes = mock(AlmacenService.class);
        politicas = mock(PoliticaStockService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProductoController(productos),
                new AlmacenController(almacenes), new PoliticaStockController(politicas))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }
    @Test void crearAlmacenDevuelve201YLocation() throws Exception {
        var almacen = new Almacen("A04", "Nuevo");
        ReflectionTestUtils.setField(almacen, "id", 4L);
        when(almacenes.crear(any())).thenReturn(almacen);
        mvc.perform(post("/api/v1/almacenes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"A04\",\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/almacenes/4"))
                .andExpect(jsonPath("$.activo").value(true));
    }
    @Test void almacenSinNombreNoLlegaAlServicio() throws Exception {
        mvc.perform(post("/api/v1/almacenes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"A04\",\"nombre\":\" \"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(almacenes);
    }
    @Test void politicaConMinimoNegativoNoLlegaAlServicio() throws Exception {
        mvc.perform(put("/api/v1/productos/1/almacenes/2/politica-stock").contentType(MediaType.APPLICATION_JSON)
                .content("{\"minimo\":-1,\"maximo\":10}")).andExpect(status().isBadRequest());
        verifyNoInteractions(politicas);
    }
    @Test void politicaSinMaximoNoLlegaAlServicio() throws Exception {
        mvc.perform(put("/api/v1/productos/1/almacenes/2/politica-stock").contentType(MediaType.APPLICATION_JSON)
                .content("{\"minimo\":0}")).andExpect(status().isBadRequest());
        verifyNoInteractions(politicas);
    }
    @Test void politicaValidaRetornaLimites() throws Exception {
        when(politicas.guardar(eq(1L), eq(2L), any())).thenReturn(new PoliticaStockResponse(3L, 1L, 2L, 0, 10));
        mvc.perform(put("/api/v1/productos/1/almacenes/2/politica-stock").contentType(MediaType.APPLICATION_JSON)
                .content("{\"minimo\":0,\"maximo\":10}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.maximo").value(10));
    }
    @Test void productoInexistenteAlDesactivarDevuelve404() throws Exception {
        when(productos.desactivar(99L)).thenThrow(new NoSuchElementException("Producto no encontrado"));
        mvc.perform(patch("/api/v1/productos/99/desactivar")).andExpect(status().isNotFound());
    }
}
