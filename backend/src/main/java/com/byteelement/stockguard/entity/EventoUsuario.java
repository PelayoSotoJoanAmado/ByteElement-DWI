package com.byteelement.stockguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "evento_usuario")
public class EventoUsuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_usuario_id")
  private Usuario actor;

  private String actorUsername;
  private String usuarioUsername;

  @Column(length = 40)
  private String tipo;

  private Instant fecha;

  @Column(length = 1000)
  private String detalle;

  protected EventoUsuario() {}

  public EventoUsuario(Usuario usuario, Usuario actor, String tipo, String detalle) {
    this.usuario = usuario;
    this.actor = actor;
    actorUsername = actor == null ? "SISTEMA" : actor.getUsername();
    usuarioUsername = usuario.getUsername();
    this.tipo = tipo;
    this.detalle = detalle;
    fecha = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
  }

  public Long getId() {
    return id;
  }

  public Long getUsuarioId() {
    return usuario.getId();
  }

  public Long getActorId() {
    return actor == null ? null : actor.getId();
  }

  public String getActorUsername() {
    return actorUsername;
  }

  public String getUsuarioUsername() {
    return usuarioUsername;
  }

  public String getTipo() {
    return tipo;
  }

  public Instant getFecha() {
    return fecha;
  }

  public String getDetalle() {
    return detalle;
  }
}
