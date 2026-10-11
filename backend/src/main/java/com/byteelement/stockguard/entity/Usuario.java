package com.byteelement.stockguard.entity;

import com.byteelement.stockguard.seguridad.Rol;
import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 80, nullable = false, unique = true)
  private String username;

  @Column(length = 100, nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(length = 30, nullable = false)
  private Rol rol;

  private boolean activo = true;

  protected Usuario() {}

  public Usuario(String username, String hash, Rol rol) {
    this.username = username;
    this.passwordHash = hash;
    this.rol = rol;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Rol getRol() {
    return rol;
  }

  public boolean isActivo() {
    return activo;
  }
}
