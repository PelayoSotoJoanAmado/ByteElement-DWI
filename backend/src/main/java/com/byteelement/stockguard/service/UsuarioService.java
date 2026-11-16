package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.dto.CuentasDtos.*;
import com.byteelement.stockguard.entity.*;
import com.byteelement.stockguard.exception.ConflictoCatalogoException;
import com.byteelement.stockguard.repository.*;
import com.byteelement.stockguard.seguridad.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {
  private final UsuarioRepository users;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final ControlCuentasRepository control;
  private final EventoUsuarioRepository eventos;
  private final String issuer;
  private final long ttl;

  public UsuarioService(
      UsuarioRepository users,
      PasswordEncoder passwords,
      JwtEncoder encoder,
      ControlCuentasRepository control,
      EventoUsuarioRepository eventos,
      @Value("${stockguard.jwt.issuer}") String issuer,
      @Value("${stockguard.jwt.ttl-minutes}") long ttl) {
    this.users = users;
    this.passwords = passwords;
    this.encoder = encoder;
    this.control = control;
    this.eventos = eventos;
    this.issuer = issuer;
    this.ttl = ttl;
  }

  public Map<String, Object> login(LoginRequest r) {
    var u =
        users
            .findByUsername(r.username())
            .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));
    if (!u.isActivo() || !passwords.matches(r.password(), u.getPasswordHash()))
      throw new BadCredentialsException("Credenciales invalidas");
    return sesion(u);
  }

  private Map<String, Object> sesion(Usuario u) {
    Instant ahora = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(u.getUsername())
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(ttl * 60))
            .claim("rol", u.getRol().name())
            .claim("uid", u.getId())
            .claim("ver", u.getVersionSesion())
            .claim("requiereCambioPassword", u.isRequiereCambioPassword())
            .build();
    String token =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return Map.of(
        "accessToken",
        token,
        "tokenType",
        "Bearer",
        "expiresIn",
        ttl * 60,
        "usuario",
        UsuarioResponse.desde(u));
  }

  private Usuario actor() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated())
      throw new BadCredentialsException("Sesion requerida");
    var u =
        users
            .findByUsername(auth.getName())
            .filter(Usuario::isActivo)
            .orElseThrow(() -> new BadCredentialsException("Sesion invalida"));
    if (auth instanceof JwtAuthenticationToken j) {
      Object uid = j.getToken().getClaim("uid"), v = j.getToken().getClaim("ver");
      if (!(uid instanceof Number)
          || !(v instanceof Number)
          || ((Number) uid).longValue() != u.getId()
          || ((Number) v).longValue() != u.getVersionSesion())
        throw new BadCredentialsException("Sesion revocada");
    }
    return u;
  }

  private Usuario administrador() {
    var u = actor();
    if (u.getRol() != Rol.ADMINISTRADOR || u.isRequiereCambioPassword())
      throw new AccessDeniedException("Administrador con password definitivo requerido");
    return u;
  }

  @PreAuthorize("isAuthenticated()")
  public UsuarioResponse miCuenta() {
    return UsuarioResponse.desde(actor());
  }

  @Transactional
  @PreAuthorize("isAuthenticated()")
  public Map<String, Object> cambiarPassword(CambiarPassword r) {
    control.bloquear();
    var u = actor();
    PoliticaPassword.confirmar(r.passwordNueva(), r.confirmacionPassword());
    if (!passwords.matches(r.passwordActual(), u.getPasswordHash()))
      throw new IllegalArgumentException("Password actual incorrecto");
    if (passwords.matches(r.passwordNueva(), u.getPasswordHash()))
      throw new IllegalArgumentException("El password nuevo debe ser diferente al actual");
    u.cambiarPassword(passwords.encode(r.passwordNueva()), false);
    users.flush();
    eventos.save(
        new EventoUsuario(
            u,
            u,
            "CAMBIO_PASSWORD",
            "Password cambiado por su titular; sesiones anteriores revocadas"));
    return sesion(u);
  }

  /** Inicializador demo y fixtures internos. HTTP usa crearAdministrado para exigir actor. */
  @Transactional
  public UsuarioResponse crear(CrearUsuarioRequest r) {
    control.bloquear();
    return crearConActor(r, null);
  }

  @Transactional
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public UsuarioResponse crearAdministrado(CrearUsuarioRequest r) {
    control.bloquear();
    return crearConActor(r, administrador());
  }

  private UsuarioResponse crearConActor(CrearUsuarioRequest r, Usuario actor) {
    PoliticaPassword.validar(r.password());
    if (r.username() == null || !r.username().matches("[a-zA-Z0-9._-]{3,80}") || r.rol() == null)
      throw new IllegalArgumentException("Usuario o rol invalido");
    if (users.findByUsername(r.username()).isPresent())
      throw new ConflictoCatalogoException("Usuario ya existe");
    var u = users.saveAndFlush(new Usuario(r.username(), passwords.encode(r.password()), r.rol()));
    eventos.save(
        new EventoUsuario(u, actor, "CREACION", "Cuenta creada; cambio de password obligatorio"));
    return UsuarioResponse.desde(u);
  }

  public List<UsuarioResponse> listar() {
    return users.findAll().stream().map(UsuarioResponse::desde).toList();
  }

  @Transactional
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public UsuarioResponse actualizar(Long id, ActualizarUsuario r) {
    control.bloquear();
    var a = administrador();
    var u = objetivo(id, a, r.version());
    if (users.findByUsername(r.username()).filter(x -> !x.getId().equals(id)).isPresent())
      throw new ConflictoCatalogoException("Usuario ya existe");
    if (u.isActivo()
        && u.getRol() == Rol.ADMINISTRADOR
        && (!r.activo() || r.rol() != Rol.ADMINISTRADOR)
        && users.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1)
      throw new ConflictoCatalogoException("Debe conservarse al menos un administrador activo");
    String antes = u.getUsername() + "/" + u.getRol() + "/activo=" + u.isActivo();
    u.actualizar(r.username(), r.rol(), r.activo());
    users.flush();
    eventos.save(
        new EventoUsuario(
            u,
            a,
            "ACTUALIZACION",
            "Antes: "
                + antes
                + "; despues: "
                + u.getUsername()
                + "/"
                + u.getRol()
                + "/activo="
                + u.isActivo()
                + ". Motivo: "
                + r.motivo()));
    return UsuarioResponse.desde(u);
  }

  @Transactional
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public UsuarioResponse restablecerPassword(Long id, RestablecerPassword r) {
    control.bloquear();
    var a = administrador();
    var u = objetivo(id, a, r.version());
    PoliticaPassword.confirmar(r.passwordTemporal(), r.confirmacionPassword());
    if (passwords.matches(r.passwordTemporal(), u.getPasswordHash()))
      throw new IllegalArgumentException("El password temporal debe ser diferente al actual");
    u.cambiarPassword(passwords.encode(r.passwordTemporal()), true);
    users.flush();
    eventos.save(
        new EventoUsuario(
            u,
            a,
            "RESTABLECIMIENTO_PASSWORD",
            "Password temporal establecido; sesiones revocadas. Motivo: " + r.motivo()));
    return UsuarioResponse.desde(u);
  }

  private Usuario objetivo(Long id, Usuario actor, Long version) {
    if (id.equals(actor.getId()))
      throw new ConflictoCatalogoException(
          "No puede administrar su propia cuenta; use Mi cuenta para cambiar password");
    var u =
        users.bloquear(id).orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    if (version == null || version != u.getVersion())
      throw new ConflictoCatalogoException(
          "La cuenta cambio; recargue su version antes de continuar");
    return u;
  }

  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public Page<Evento> eventos(Long id, int pagina, int tamanio) {
    administrador();
    if (!users.existsById(id)) throw new NoSuchElementException("Usuario no encontrado");
    return eventos
        .findByUsuarioId(id, PageRequest.of(pagina, tamanio, Sort.by("fecha", "id").descending()))
        .map(Evento::desde);
  }
}
