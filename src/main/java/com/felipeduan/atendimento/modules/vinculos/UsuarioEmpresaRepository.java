package com.felipeduan.atendimento.modules.vinculos;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, UsuarioEmpresaId> {

  Optional<UsuarioEmpresa> findByIdUsuarioId(UUID usuarioId);

  Optional<UsuarioEmpresa> findByIdUsuarioIdAndStatus(UUID usuarioId, String status);

  Page<UsuarioEmpresa> findByStatus(String status, Pageable pageable);

  long countByPerfilAndStatus(String perfil, String status);
}
