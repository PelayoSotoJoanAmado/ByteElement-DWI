package com.byteelement.stockguard.seguridad;

import com.byteelement.stockguard.repository.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecretKey secretKey(@Value("${stockguard.jwt.secret}") String secret) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
      throw new IllegalArgumentException(
          "JWT_SECRET debe tener al menos 32 bytes y suministrarse externamente");
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder jwtDecoder(
      SecretKey key, UsuarioRepository users, @Value("${stockguard.jwt.issuer}") String issuer) {
    var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return token -> {
      Jwt jwt = decoder.decode(token);
      var u =
          users
              .findByUsername(jwt.getSubject())
              .orElseThrow(() -> new BadJwtException("Usuario inválido"));
      if (!u.isActivo() || !u.getRol().name().equals(jwt.getClaimAsString("rol")))
        throw new BadJwtException("Usuario o permisos inválidos");
      return jwt;
    };
  }

  @Bean
  SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
    var authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("rol");
    authorities.setAuthorityPrefix("ROLE_");
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers("/api/v1/usuarios/**")
                    .hasRole("ADMINISTRADOR")
                    .requestMatchers(HttpMethod.POST, "/api/v1/productos", "/api/v1/almacenes")
                    .hasRole("ADMINISTRADOR")
                    .requestMatchers(
                        HttpMethod.PUT, "/api/v1/productos/*/almacenes/*/politica-stock")
                    .hasRole("SUPERVISOR")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/productos/*", "/api/v1/almacenes/*")
                    .hasRole("ADMINISTRADOR")
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/v1/productos/*/desactivar",
                        "/api/v1/almacenes/*/desactivar")
                    .hasRole("ADMINISTRADOR")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/movimientos",
                        "/api/v1/transferencias",
                        "/api/v1/conteos")
                    .hasRole("ALMACEN")
                    .requestMatchers(
                        HttpMethod.POST, "/api/v1/conteos/*/confirmar", "/api/v1/cortes-stock")
                    .hasRole("SUPERVISOR")
                    .requestMatchers(
                        HttpMethod.GET, "/api/v1/indicadores", "/api/v1/cortes-stock/**")
                    .hasAnyRole("SUPERVISOR", "ADMINISTRADOR")
                    .requestMatchers(HttpMethod.GET, "/api/v1/conteos/**")
                    .hasAnyRole("ALMACEN", "SUPERVISOR")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/productos",
                        "/api/v1/productos/*",
                        "/api/v1/productos/*/almacenes/*/politica-stock",
                        "/api/v1/almacenes",
                        "/api/v1/almacenes/*",
                        "/api/v1/existencias",
                        "/api/v1/movimientos",
                        "/api/v1/movimientos/*",
                        "/api/v1/reportes/**")
                    .hasAnyRole(
                        "ALMACEN", "COMPRAS", "SUPERVISOR", "CONSULTA_COMERCIAL", "ADMINISTRADOR")
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))
        .build();
  }
}
