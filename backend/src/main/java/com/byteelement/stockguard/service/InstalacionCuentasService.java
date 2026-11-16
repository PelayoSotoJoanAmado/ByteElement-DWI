package com.byteelement.stockguard.service;

import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.repository.*;
import com.byteelement.stockguard.seguridad.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstalacionCuentasService {
  private final CuentasInicialesProperties config;
  private final UsuarioRepository usuarios;
  private final ControlCuentasRepository control;
  private final EventoUsuarioRepository eventos;
  private final PasswordEncoder passwords;

  public InstalacionCuentasService(
      CuentasInicialesProperties c,
      UsuarioRepository u,
      ControlCuentasRepository l,
      EventoUsuarioRepository e,
      PasswordEncoder p) {
    config = c;
    usuarios = u;
    control = l;
    eventos = e;
    passwords = p;
  }

  @Transactional
  public boolean inicializar() {
    if (!config.isEnabled()) return false;
    var estado = control.bloquear();
    if (usuarios.count() != 0) {
      estado.completar();
      return false;
    }
    if (estado.isInstalacionCompleta())
      throw new IllegalStateException(
          "Instalacion marcada completa sin usuarios: revisar recuperacion de cuentas");
    var cuentas = config.cuentas();
    var nombres = new HashSet<String>();
    var claves = new HashSet<String>();
    for (var entry : cuentas.entrySet()) {
      var c = entry.getValue();
      if (c == null
          || c.getUsername() == null
          || !c.getUsername().matches("[a-zA-Z0-9._-]{3,80}")
          || !nombres.add(c.getUsername()))
        throw new IllegalArgumentException(
            "Nombre inicial invalido o duplicado para " + entry.getKey());
      try {
        PoliticaPassword.validar(c.getPassword());
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException(
            "Configure una contrasena inicial privada valida para " + entry.getKey());
      }
      if (c.getPassword().startsWith("REEMPLAZAR") || !claves.add(c.getPassword()))
        throw new IllegalArgumentException(
            "Las cinco contrasenas iniciales deben ser distintas y no usar marcadores");
    }
    for (var entry : cuentas.entrySet()) {
      var c = entry.getValue();
      var u =
          usuarios.saveAndFlush(
              new Usuario(c.getUsername(), passwords.encode(c.getPassword()), entry.getKey()));
      eventos.save(
          new EventoUsuario(
              u, null, "INSTALACION", "Cuenta inicial; cambio de password obligatorio"));
    }
    estado.completar();
    return true;
  }
}
