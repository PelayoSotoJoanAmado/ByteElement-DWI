package com.byteelement.stockguard.dto;

import com.byteelement.stockguard.entity.Usuario;
import com.byteelement.stockguard.seguridad.Rol;

public record UsuarioResponse(Long id, String username, Rol rol, boolean activo) {
  public static UsuarioResponse desde(Usuario u) {
    return new UsuarioResponse(u.getId(), u.getUsername(), u.getRol(), u.isActivo());
  }
}
