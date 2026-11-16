package com.byteelement.stockguard.seguridad;

import java.util.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stockguard.bootstrap")
public class CuentasInicialesProperties {
  private boolean enabled = true;
  private String username = "administrador", password = "";
  private Cuenta almacen = new Cuenta("almacen"),
      compras = new Cuenta("compras"),
      supervisor = new Cuenta("supervisor"),
      consulta = new Cuenta("consulta-comercial");

  public static class Cuenta {
    private String username, password = "";

    public Cuenta() {}

    public Cuenta(String username) {
      this.username = username;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String v) {
      username = v;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String v) {
      password = v;
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean v) {
    enabled = v;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String v) {
    username = v;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String v) {
    password = v;
  }

  public Cuenta getAlmacen() {
    return almacen;
  }

  public void setAlmacen(Cuenta v) {
    almacen = v;
  }

  public Cuenta getCompras() {
    return compras;
  }

  public void setCompras(Cuenta v) {
    compras = v;
  }

  public Cuenta getSupervisor() {
    return supervisor;
  }

  public void setSupervisor(Cuenta v) {
    supervisor = v;
  }

  public Cuenta getConsulta() {
    return consulta;
  }

  public void setConsulta(Cuenta v) {
    consulta = v;
  }

  public Map<Rol, Cuenta> cuentas() {
    var map = new EnumMap<Rol, Cuenta>(Rol.class);
    var admin = new Cuenta(username);
    admin.setPassword(password);
    map.put(Rol.ADMINISTRADOR, admin);
    map.put(Rol.ALMACEN, almacen);
    map.put(Rol.COMPRAS, compras);
    map.put(Rol.SUPERVISOR, supervisor);
    map.put(Rol.CONSULTA_COMERCIAL, consulta);
    return map;
  }
}
