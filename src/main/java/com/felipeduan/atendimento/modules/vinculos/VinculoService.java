package com.felipeduan.atendimento.modules.vinculos;

import com.felipeduan.atendimento.modules.usuarios.exception.UltimoAdministradorException;
import com.felipeduan.atendimento.modules.usuarios.exception.UsuarioJaVinculadoException;
import com.felipeduan.atendimento.modules.usuarios.exception.UsuarioNaoEncontradoException;
import com.felipeduan.atendimento.shared.security.Roles;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import com.felipeduan.atendimento.shared.tenancy.TenantRlsConfigurer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    return vincular(usuarioId, empresaId, Roles.ADMINISTRADOR);
  }

  @Transactional
  public UsuarioEmpresa vincular(UUID usuarioId, UUID empresaId, String perfil) {
    UsuarioEmpresaId id = new UsuarioEmpresaId(usuarioId, empresaId);
    if (usuarioEmpresaRepository.existsById(id)) {
      throw new UsuarioJaVinculadoException();
    }
    return usuarioEmpresaRepository.save(new UsuarioEmpresa(usuarioId, empresaId, perfil));
  }

  @Transactional(readOnly = true)
  public Page<UsuarioEmpresa> listarVinculosDoTenant(Pageable pageable) {
    return usuarioEmpresaRepository.findByStatus(STATUS_ATIVO, pageable);
  }

  @Transactional(readOnly = true)
  public Optional<UsuarioEmpresa> buscarVinculo(UUID usuarioId) {
    return usuarioEmpresaRepository.findByIdUsuarioIdAndStatus(usuarioId, STATUS_ATIVO);
  }

  @Transactional(readOnly = true)
  public Optional<UsuarioEmpresa> buscarVinculoQualquerStatus(UUID usuarioId) {
    return usuarioEmpresaRepository.findByIdUsuarioId(usuarioId);
  }

  @Transactional
  public UsuarioEmpresa atualizarVinculo(UUID usuarioId, String novoPerfil, String novoStatus) {
    UsuarioEmpresa vinculo =
        usuarioEmpresaRepository
            .findByIdUsuarioId(usuarioId)
            .orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));

    exigirOutroAdministradorAtivo(vinculo, novoPerfil, novoStatus);
    vinculo.atualizar(novoPerfil, novoStatus);
    return usuarioEmpresaRepository.save(vinculo);
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

  private void exigirOutroAdministradorAtivo(
      UsuarioEmpresa vinculo, String novoPerfil, String novoStatus) {
    boolean deixaDeSerAdminAtivo =
        vinculo.ehAdministradorAtivo()
            && (!Roles.ADMINISTRADOR.equals(novoPerfil) || !STATUS_ATIVO.equals(novoStatus));

    if (deixaDeSerAdminAtivo
        && usuarioEmpresaRepository.countByPerfilAndStatus(Roles.ADMINISTRADOR, STATUS_ATIVO)
            <= 1) {
      throw new UltimoAdministradorException();
    }
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
