package com.felipeduan.atendimento.modules.empresas;

import com.felipeduan.atendimento.modules.empresas.dto.AdminInicialRequest;
import com.felipeduan.atendimento.modules.empresas.dto.AtualizarEmpresaRequest;
import com.felipeduan.atendimento.modules.empresas.dto.CriarEmpresaRequest;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResumoResponse;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResponse;
import com.felipeduan.atendimento.modules.empresas.exception.CnpjJaCadastradoException;
import com.felipeduan.atendimento.modules.empresas.exception.EmpresaNaoEncontradaException;
import com.felipeduan.atendimento.modules.usuarios.Usuario;
import com.felipeduan.atendimento.modules.usuarios.UsuarioService;
import com.felipeduan.atendimento.modules.vinculos.VinculoService;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
  public PageResponse<EmpresaResumoResponse> listarAtivas(Pageable pageable) {
    Page<EmpresaResumoResponse> pagina =
        empresaRepository
            .findByStatus(EmpresaStatus.ATIVA, pageable)
            .map(empresaMapper::toResumoResponse);
    return PageResponse.of(pagina);
  }

  @Transactional(readOnly = true)
  public PageResponse<EmpresaResumoResponse> listarInativas(Pageable pageable) {
    Page<Object[]> paginaBruta =
        empresaRepository.findPaginaResumoPorStatus(EmpresaStatus.INATIVA.name(), pageable);

    List<EmpresaResumoResponse> conteudo = new ArrayList<>();
    for (Object[] linha : paginaBruta.getContent()) {
      conteudo.add(mapearResumo(linha));
    }

    Page<EmpresaResumoResponse> pagina =
        new PageImpl<>(conteudo, pageable, paginaBruta.getTotalElements());
    return PageResponse.of(pagina);
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

    Page<Empresa> pagina =
        empresaRepository.findByStatus(EmpresaStatus.ATIVA, Pageable.unpaged());
    for (Empresa empresa : pagina.getContent()) {
      empresasAtivas.add(empresa.getId());
    }

    return empresasAtivas;
  }

  private EmpresaResumoResponse mapearResumo(Object[] linha) {
    Instant dataCriacao = extrairInstant(linha[6]);

    return new EmpresaResumoResponse(
        (UUID) linha[0],
        (String) linha[1],
        (String) linha[2],
        (String) linha[3],
        EmpresaStatus.valueOf((String) linha[4]),
        (String) linha[5],
        dataCriacao);
  }

  private Instant extrairInstant(Object valor) {
    return switch (valor) {
      case Instant instant -> instant;
      case Timestamp timestamp -> timestamp.toInstant();
      case java.time.OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
      case java.util.Date date -> date.toInstant();
      default ->
          throw new IllegalStateException(
              "Tipo temporal não suportado: " + valor.getClass().getName());
    };
  }

  @Transactional(readOnly = true)
  public Empresa buscarPorId(UUID id) {
    return empresaRepository.findById(id).orElseThrow(() -> new EmpresaNaoEncontradaException(id));
  }
}
