package com.byteelement.stockguard.service;

import java.time.Instant;

public final class Periodo {
  private Periodo() {}

  public static void validar(Instant desde, Instant hasta) {
    if (desde == null || hasta == null || !desde.isBefore(hasta))
      throw new IllegalArgumentException("El período debe cumplir desde < hasta (hasta exclusivo)");
  }
}
