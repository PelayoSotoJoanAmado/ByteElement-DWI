package com.byteelement.stockguard.seguridad;

import com.byteelement.stockguard.service.InstalacionCuentasService;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Inicializa las cinco cuentas en una unica transaccion durante la primera instalacion. */
@Component
@Order(0)
public class BootstrapAdmin implements ApplicationRunner {
  private final InstalacionCuentasService instalacion;

  public BootstrapAdmin(InstalacionCuentasService i) {
    instalacion = i;
  }

  @Override
  public void run(ApplicationArguments args) {
    instalacion.inicializar();
  }
}
