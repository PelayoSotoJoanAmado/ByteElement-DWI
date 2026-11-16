package com.byteelement.stockguard.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(
    properties = {
      "spring.config.location=optional:file:./src/main/resources/application.properties",
      "spring.profiles.active=prod",
      "spring.datasource.url=jdbc:h2:mem:perfil-prod;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "stockguard.jwt.secret=production-profile-test-secret-at-least-32-bytes",
      "stockguard.bootstrap.username=perfil-admin",
      "stockguard.bootstrap.password=ProfileTestPassword123!",
      "debug=false",
      "stockguard.bootstrap.enabled=false"
    })
class PerfilProduccionIntegrationTest {
  @Autowired Environment entorno;

  @Test
  void produccionNoImportaLaConfiguracionLocalDelDesarrollador() {
    assertArrayEquals(new String[] {"prod"}, entorno.getActiveProfiles());
    assertTrue(entorno.getProperty("spring.config.import", "").isBlank());
    assertFalse(entorno.getProperty("debug", Boolean.class, false));
    assertEquals("INFO", entorno.getProperty("logging.level.org.springframework.web"));
  }
}
