package com.felipeduan.atendimento.modules.vinculos;

import com.felipeduan.atendimento.shared.security.Roles;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import com.felipeduan.atendimento.shared.tenancy.TenantRlsConfigurer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VinculoService {

  private static final String STATUS_ATIVO = "ATIVO";

  private final UsuarioEmpresaRepository usuarioEmpresaRepository;
  private final TenantRlsConfigurer tenantRlsConfigurer;

  @Transactional
  public UsuarioEmpresa vincularComoAdministrador(UUID usuarioId, UUID empresaId) {
    var usuarioEmpresa = new UsuarioEmpresa(usuarioId, empresaId, Roles.ADMINISTRADOR);
    return usuarioEmpresaRepository.save(usuarioEmpresa);
  }

  @Transactional(readOnly = true)
  public List<UsuarioEmpresa> listarVinculosAtivosDoUsuario(
      UUID usuarioId, List<UUID> empresasAtivasIds) {
    List<UsuarioEmpresa> vinculosAtivos = new ArrayList<>();

    for (UUID empresaId : empresasAtivasIds) {
      TenantContext.withTenantId(
          empresaId,
          () -> consultarVinculoAtivo(usuarioId, empresaId).ifPresent(vinculosAtivos::add));
    }

    return vinculosAtivos;
  }

  @Transactional(readOnly = true)
  public Optional<UsuarioEmpresa> buscarVinculoAtivo(UUID usuarioId, UUID empresaId) {
    return TenantContext.withTenantId(empresaId, () -> consultarVinculoAtivo(usuarioId, empresaId));
  }

  private Optional<UsuarioEmpresa> consultarVinculoAtivo(UUID usuarioId, UUID empresaId) {
    tenantRlsConfigurer.aplicar(empresaId);

    Optional<UsuarioEmpresa> vinculo =
        usuarioEmpresaRepository.findById(new UsuarioEmpresaId(usuarioId, empresaId));

    if (vinculo.isEmpty()) {
      return Optional.empty();
    }

    UsuarioEmpresa vinculoEncontrado = vinculo.get();
    if (!STATUS_ATIVO.equals(vinculoEncontrado.getStatus())) {
      return Optional.empty();
    }

    return Optional.of(vinculoEncontrado);
  }

  public UUID resolverEmpresaPadrao(UUID lastEmpresaId, List<UsuarioEmpresa> vinculos) {
    if (vinculos.size() == 1) {
      return vinculos.getFirst().getId().empresaId();
    }

    if (lastEmpresaId != null) {
      for (UsuarioEmpresa vinculo : vinculos) {
        if (vinculo.getId().empresaId().equals(lastEmpresaId)) {
          return lastEmpresaId;
        }
      }
    }

    UsuarioEmpresa vinculoMaisRecente = vinculos.getFirst();
    for (UsuarioEmpresa vinculo : vinculos) {
      if (vinculo.getDataVinculo().isAfter(vinculoMaisRecente.getDataVinculo())) {
        vinculoMaisRecente = vinculo;
      }
    }

    return vinculoMaisRecente.getId().empresaId();
  }
}
