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
  public Map<String, Object> login(@Valid @RequestBody LoginRequest r) {
    return usuarios.login(r);
  }

  @PostMapping("/usuarios")
  public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest r) {
    var u = usuarios.crear(r);
    return ResponseEntity.created(URI.create("/api/v1/usuarios")).body(u);
  }

  @GetMapping("/usuarios")
  public List<UsuarioResponse> listar() {
    return usuarios.listar();
  }
}
