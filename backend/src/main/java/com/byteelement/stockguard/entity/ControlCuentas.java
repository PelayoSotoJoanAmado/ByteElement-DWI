package com.byteelement.stockguard.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "control_cuentas")
public class ControlCuentas {
  @Id private Long id;
  private boolean instalacionCompleta;

  protected ControlCuentas() {}

  public boolean isInstalacionCompleta() {
    return instalacionCompleta;
  }

  public void completar() {
    instalacionCompleta = true;
  }
}
