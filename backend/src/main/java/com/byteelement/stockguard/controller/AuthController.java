package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
  private final UsuarioService usuarios;

  public AuthController(UsuarioService usuarios) {
    this.usuarios = usuarios;
  }

  @PostMapping("/auth/login")
  public Map<String, Object> login(
      @Valid @RequestBody LoginRequest r, jakarta.servlet.http.HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    return usuarios.login(r);
  }

  @GetMapping("/auth/me")
  public UsuarioResponse me(jakarta.servlet.http.HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    return usuarios.miCuenta();
  }

  @PostMapping("/auth/cambiar-password")
  public Map<String, Object> cambiar(
      @Valid @RequestBody CuentasDtos.CambiarPassword r,
      jakarta.servlet.http.HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    return usuarios.cambiarPassword(r);
  }

  @PutMapping("/usuarios/{id}")
  public UsuarioResponse actualizar(
      @PathVariable Long id, @Valid @RequestBody CuentasDtos.ActualizarUsuario r) {
    return usuarios.actualizar(id, r);
  }

  @PostMapping("/usuarios/{id}/restablecer-password")
  public UsuarioResponse restablecer(
      @PathVariable Long id, @Valid @RequestBody CuentasDtos.RestablecerPassword r) {
    return usuarios.restablecerPassword(id, r);
  }

  @GetMapping("/usuarios/{id}/eventos")
  public org.springframework.data.domain.Page<CuentasDtos.Evento> eventos(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int pagina,
      @RequestParam(defaultValue = "20")
          @jakarta.validation.constraints.Min(1)
          @jakarta.validation.constraints.Max(100)
          int tamanio) {
    return usuarios.eventos(id, pagina, tamanio);
  }

  @PostMapping("/usuarios")
  public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest r) {
    var u = usuarios.crearAdministrado(r);
    return ResponseEntity.created(URI.create("/api/v1/usuarios")).body(u);
  }

  @GetMapping("/usuarios")
  public List<UsuarioResponse> listar() {
    return usuarios.listar();
  }
}
