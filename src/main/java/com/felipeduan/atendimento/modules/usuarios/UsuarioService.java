package com.felipeduan.atendimento.modules.usuarios;

import com.felipeduan.atendimento.modules.usuarios.dto.AtualizarUsuarioRequest;
import com.felipeduan.atendimento.modules.usuarios.dto.CriarUsuarioRequest;
import com.felipeduan.atendimento.modules.usuarios.dto.UsuarioResponse;
import com.felipeduan.atendimento.modules.usuarios.enums.PerfilUsuario;
import com.felipeduan.atendimento.modules.usuarios.enums.StatusVinculo;
import com.felipeduan.atendimento.modules.usuarios.exception.EmailExistenteSenhaInvalidaException;
import com.felipeduan.atendimento.modules.usuarios.exception.UsuarioNaoEncontradoException;
import com.felipeduan.atendimento.modules.vinculos.UsuarioEmpresa;
import com.felipeduan.atendimento.modules.vinculos.VinculoService;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

  private final UsuarioRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final VinculoService vinculoService;

  @Transactional(readOnly = true)
  public Optional<Usuario> buscarPorEmail(String email) {
    return repository.findByEmail(email);
  }

  @Transactional(readOnly = true)
  public Optional<Usuario> buscarPorId(UUID id) {
    return repository.findById(id);
  }

  @Transactional
  public Usuario salvar(Usuario usuario) {
    return repository.save(usuario);
  }

  @Transactional
  public Usuario resolverOuCriarComSenhaTemporaria(
      String nome, String email, String senhaTemporaria, UUID empresaId) {

    return repository
        .findByEmail(email)
        .map(existente -> reutilizarContaExistente(existente, senhaTemporaria))
        .orElseGet(() -> criarContaNova(nome, email, senhaTemporaria, empresaId));
  }

  @Transactional
  public UsuarioResponse criar(CriarUsuarioRequest request) {
    UUID empresaId = TenantContext.exigirTenantId();
    Usuario usuario =
        resolverOuCriarComSenhaTemporaria(
            request.nome(), request.email(), request.senha(), empresaId);
    UsuarioEmpresa vinculo =
        vinculoService.vincular(usuario.getId(), empresaId, request.perfil().name());
    return montarResposta(usuario, vinculo);
  }

  @Transactional(readOnly = true)
  public PageResponse<UsuarioResponse> listar(Pageable pageable) {
    Page<UsuarioEmpresa> vinculos = vinculoService.listarVinculosDoTenant(pageable);
    Map<UUID, Usuario> usuariosPorId = indexarUsuarios(vinculos.getContent());
    return PageResponse.of(
        vinculos.map(vinculo -> montarRespostaDaListagem(vinculo, usuariosPorId)));
  }

  @Transactional(readOnly = true)
  public UsuarioResponse buscar(UUID usuarioId) {
    UsuarioEmpresa vinculo =
        vinculoService
            .buscarVinculo(usuarioId)
            .orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));
    return montarResposta(carregar(usuarioId), vinculo);
  }

  @Transactional
  public UsuarioResponse atualizar(UUID usuarioId, AtualizarUsuarioRequest request) {
    UsuarioEmpresa vinculo =
        vinculoService.atualizarVinculo(
            usuarioId, request.perfil().name(), request.status().name());
    Usuario usuario = carregar(usuarioId);
    usuario.atualizarNome(request.nome());
    repository.save(usuario);
    return montarResposta(usuario, vinculo);
  }

  private Usuario reutilizarContaExistente(Usuario usuario, String senha) {
    if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
      throw new EmailExistenteSenhaInvalidaException();
    }

    return usuario;
  }

  private Usuario criarContaNova(String nome, String email, String senha, UUID empresaId) {
    return repository.save(
        Usuario.criarComSenhaTemporaria(nome, email, passwordEncoder.encode(senha), empresaId));
  }

  private Usuario carregar(UUID usuarioId) {
    return repository
        .findById(usuarioId)
        .orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));
  }

  private Map<UUID, Usuario> indexarUsuarios(List<UsuarioEmpresa> vinculos) {
    List<UUID> ids = vinculos.stream().map(v -> v.getId().usuarioId()).distinct().toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return repository.findByIdIn(ids).stream()
        .collect(Collectors.toMap(Usuario::getId, Function.identity()));
  }

  private UsuarioResponse montarRespostaDaListagem(
      UsuarioEmpresa vinculo, Map<UUID, Usuario> usuariosPorId) {

    Usuario usuario = usuariosPorId.get(vinculo.getId().usuarioId());
    if (usuario == null) {
      throw new UsuarioNaoEncontradoException(vinculo.getId().usuarioId());
    }
    return montarResposta(usuario, vinculo);
  }

  private UsuarioResponse montarResposta(Usuario usuario, UsuarioEmpresa vinculo) {
    return new UsuarioResponse(
        usuario.getId(),
        usuario.getNome(),
        usuario.getEmail(),
        PerfilUsuario.valueOf(vinculo.getPerfil()),
        StatusVinculo.valueOf(vinculo.getStatus()),
        usuario.getDataCriacao());
  }
}
