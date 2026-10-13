package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.*;
import com.byteelement.stockguard.entity.Usuario;
import com.byteelement.stockguard.exception.ConflictoCatalogoException;
import com.byteelement.stockguard.repository.UsuarioRepository;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {
  private final UsuarioRepository users;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final String issuer;
  private final long ttl;

  public UsuarioService(
      UsuarioRepository users,
      PasswordEncoder passwords,
      JwtEncoder encoder,
      @Value("${stockguard.jwt.issuer}") String issuer,
      @Value("${stockguard.jwt.ttl-minutes}") long ttl) {
    this.users = users;
    this.passwords = passwords;
    this.encoder = encoder;
    this.issuer = issuer;
    this.ttl = ttl;
  }

  public Map<String, Object> login(LoginRequest r) {
    var u =
        users
            .findByUsername(r.username())
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
    if (!u.isActivo() || !passwords.matches(r.password(), u.getPasswordHash()))
      throw new BadCredentialsException("Credenciales inválidas");
    Instant ahora = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(u.getUsername())
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(ttl * 60))
            .claim("rol", u.getRol().name())
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

  @Transactional
  public UsuarioResponse crear(CrearUsuarioRequest r) {
    if (r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException("Contraseña excede 72 bytes");
    if (users.findByUsername(r.username()).isPresent())
      throw new ConflictoCatalogoException("Usuario ya existe");
    return UsuarioResponse.desde(
        users.saveAndFlush(new Usuario(r.username(), passwords.encode(r.password()), r.rol())));
  }

  public List<UsuarioResponse> listar() {
    return users.findAll().stream().map(UsuarioResponse::desde).toList();
  }
}
