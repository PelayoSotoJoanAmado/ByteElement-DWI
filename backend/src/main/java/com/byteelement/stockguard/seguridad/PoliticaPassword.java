package com.byteelement.stockguard.seguridad;

import java.nio.charset.StandardCharsets;

public final class PoliticaPassword {
  private PoliticaPassword() {}

  public static void validar(String password) {
    if (password == null
        || password.isBlank()
        || password.length() < 12
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "Password debe tener al menos 12 caracteres y como maximo 72 bytes UTF-8");
  }

  public static void confirmar(String password, String confirmacion) {
    validar(password);
    if (!password.equals(confirmacion))
      throw new IllegalArgumentException("La confirmacion de password no coincide");
  }
}
