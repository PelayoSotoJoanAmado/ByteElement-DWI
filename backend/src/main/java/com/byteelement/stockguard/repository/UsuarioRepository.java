package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id=:id")
  Optional<Usuario> bloquear(Long id);

  Optional<Usuario> findByUsername(String username);
}
