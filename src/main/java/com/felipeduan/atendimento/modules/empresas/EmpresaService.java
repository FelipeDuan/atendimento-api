package com.felipeduan.atendimento.modules.empresas;

import com.felipeduan.atendimento.modules.empresas.dto.AdminInicialRequest;
import com.felipeduan.atendimento.modules.empresas.dto.AtualizarEmpresaRequest;
import com.felipeduan.atendimento.modules.empresas.dto.CriarEmpresaRequest;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResponse;
import com.felipeduan.atendimento.modules.empresas.exception.CnpjJaCadastradoException;
import com.felipeduan.atendimento.modules.empresas.exception.EmpresaNaoEncontradaException;
import com.felipeduan.atendimento.modules.usuarios.Usuario;
import com.felipeduan.atendimento.modules.usuarios.UsuarioService;
import com.felipeduan.atendimento.modules.vinculos.VinculoService;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmpresaService {

  private final EmpresaRepository empresaRepository;
  private final UsuarioService usuarioService;
  private final VinculoService vinculoService;
  private final EmpresaMapper empresaMapper;

  @Transactional
  public EmpresaResponse criar(CriarEmpresaRequest request) {
    validarCnpjDisponivel(request.cnpj());

    Empresa empresa = salvarEmpresa(request);
    Usuario admin = resolverAdminInicial(request.administradorInicial(), empresa.getId());
    vincularAdminNaEmpresa(empresa.getId(), admin.getId());

    return empresaMapper.toResponse(empresa, admin);
  }

  @Transactional(readOnly = true)
  public EmpresaResponse buscar(UUID id) {
    return empresaMapper.toResponse(buscarPorId(id));
  }

  @Transactional
  public EmpresaResponse atualizar(UUID id, AtualizarEmpresaRequest request) {
    Empresa empresa = buscarPorId(id);
    empresa.atualizar(request.nome(), request.email(), request.phoneNumberId());
    return empresaMapper.toResponse(empresaRepository.save(empresa));
  }

  @Transactional
  public void inativar(UUID id) {
    Empresa empresa = buscarPorId(id);
    empresa.inativar();
    empresaRepository.save(empresa);
  }

  private void validarCnpjDisponivel(String cnpj) {
    if (empresaRepository.existsByCnpj(cnpj)) {
      throw new CnpjJaCadastradoException();
    }
  }

  private Empresa salvarEmpresa(CriarEmpresaRequest request) {
    return empresaRepository.save(new Empresa(request.nome(), request.cnpj(), request.email()));
  }

  private Usuario resolverAdminInicial(AdminInicialRequest admin, UUID empresaId) {
    return usuarioService.resolverOuCriarAdminInicial(
        admin.nome(), admin.email(), admin.senhaTemporaria(), empresaId);
  }

  private void vincularAdminNaEmpresa(UUID empresaId, UUID usuarioId) {
    TenantContext.withTenantId(
        empresaId, () -> vinculoService.vincularComoAdministrador(usuarioId, empresaId));
  }

  @Transactional(readOnly = true)
  public List<UUID> listarIdsEmpresasAtivas() {
    List<UUID> empresasAtivas = new ArrayList<>();

    for (Empresa empresa : empresaRepository.findByStatus(EmpresaStatus.ATIVA)) {
      empresasAtivas.add(empresa.getId());
    }

    return empresasAtivas;
  }

  @Transactional(readOnly = true)
  public Empresa buscarPorId(UUID id) {
    return empresaRepository.findById(id).orElseThrow(() -> new EmpresaNaoEncontradaException(id));
  }
}
