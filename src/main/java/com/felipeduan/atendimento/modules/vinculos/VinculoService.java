package com.felipeduan.atendimento.modules.vinculos;

import com.felipeduan.atendimento.shared.security.Roles;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VinculoService {

  private final UsuarioEmpresaRepository usuarioEmpresaRepository;

  @Transactional
  public UsuarioEmpresa vincularComoAdministrador(UUID usuarioId, UUID empresaId) {
    var usuarioEmpresa = new UsuarioEmpresa(usuarioId, empresaId, Roles.ADMINISTRADOR);
    return usuarioEmpresaRepository.save(usuarioEmpresa);
  }
}
