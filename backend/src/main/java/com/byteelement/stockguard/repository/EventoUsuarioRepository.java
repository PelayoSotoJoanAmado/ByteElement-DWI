package com.byteelement.stockguard.repository;

import com.byteelement.stockguard.entity.EventoUsuario;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoUsuarioRepository extends JpaRepository<EventoUsuario, Long> {
  @org.springframework.data.jpa.repository.Query(
      "select e from EventoUsuario e where e.usuario.id=:usuarioId")
  Page<EventoUsuario> findByUsuarioId(Long usuarioId, Pageable pageable);
}
