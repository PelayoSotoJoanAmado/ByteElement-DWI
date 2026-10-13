package com.byteelement.stockguard.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.byteelement.stockguard.config.DatosDemo;
import com.byteelement.stockguard.repository.*;
import com.byteelement.stockguard.service.ReporteService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${DEMO_TEST_DB_URL:jdbc:h2:mem:stockguard-demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
      "stockguard.demo.password=DemoTestPassword123!"
    })
@ActiveProfiles("demo")
class DatosDemoIntegrationTest {
  @Autowired DatosDemo demo;
  @Autowired MovimientoRepository movimientos;
  @Autowired ProductoRepository productos;
  @Autowired UsuarioRepository usuarios;
  @Autowired ReporteService reportes;
  @Autowired ConteoRepository conteos;

  @Test
  void escenarioInicialSeCalculaDesdePersistenciaYNoDuplicaDatosAlReiniciar() {
    assertEquals(2, productos.count());
    assertEquals(4, movimientos.count());
    assertEquals(2, conteos.count());
    assertEquals(6, usuarios.count());
    var desde = Instant.parse("1970-01-01T00:00:00Z");
    var hasta = Instant.now().plusSeconds(1);
    assertEquals(
        new BigDecimal("100.00"), reportes.indicador("I05", desde, hasta, null, null).valor());
    assertEquals(
        new BigDecimal("50.00"), reportes.indicador("I07", desde, hasta, null, null).valor());
    assertEquals(
        new BigDecimal("50.00"), reportes.indicador("I10", desde, hasta, null, null).valor());
    demo.run(new DefaultApplicationArguments());
    assertEquals(2, productos.count());
    assertEquals(4, movimientos.count());
    assertEquals(2, conteos.count());
  }
}
