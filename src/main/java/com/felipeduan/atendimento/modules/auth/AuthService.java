package com.felipeduan.atendimento.modules.auth;

import com.felipeduan.atendimento.modules.auth.dto.EmpresaVinculadaResponse;
import com.felipeduan.atendimento.modules.auth.dto.LoginRequest;
import com.felipeduan.atendimento.modules.auth.dto.LoginResponse;
import com.felipeduan.atendimento.modules.auth.dto.SwitchTenantRequest;
import com.felipeduan.atendimento.modules.auth.dto.SwitchTenantResponse;
import com.felipeduan.atendimento.modules.auth.dto.TrocarSenhaRequest;
import com.felipeduan.atendimento.modules.auth.exceptions.LoginCredenciaisInvalidasException;
import com.felipeduan.atendimento.modules.auth.exceptions.SemAcessoEmpresaException;
import com.felipeduan.atendimento.modules.auth.exceptions.SemVinculoAtivoException;
import com.felipeduan.atendimento.modules.empresas.EmpresaService;
import com.felipeduan.atendimento.modules.usuarios.Usuario;
import com.felipeduan.atendimento.modules.usuarios.UsuarioRepository;
import com.felipeduan.atendimento.modules.vinculos.UsuarioEmpresa;
import com.felipeduan.atendimento.modules.vinculos.VinculoService;
import com.felipeduan.atendimento.shared.security.JwtService;
import com.felipeduan.atendimento.shared.security.Roles;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final VinculoService vinculoService;
  private final EmpresaService empresaService;

  @Transactional
  public LoginResponse login(LoginRequest request) {
    Usuario usuario =
        usuarioRepository
            .findByEmail(request.email())
            .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
            .orElseThrow(LoginCredenciaisInvalidasException::new);

    if (usuario.isDeveTrocarSenha()) {
      String usuarioId = usuario.getId().toString();
      List<String> roles = List.of(Roles.TROCAR_SENHA);

      String token = jwtService.emitirToken(usuarioId, roles, null);

      return new LoginResponse(token, true, List.of());
    }

    return emitirTokenPosAutenticacao(usuario);
  }

  @Transactional
  public LoginResponse trocarSenha(TrocarSenhaRequest request) {
    UUID usuarioId = usuarioAutenticadoId();

    Usuario usuario =
        usuarioRepository.findById(usuarioId).orElseThrow(LoginCredenciaisInvalidasException::new);

    if (!usuario.isDeveTrocarSenha()) {
      throw new SemAcessoEmpresaException();
    }

    if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
      throw new LoginCredenciaisInvalidasException();
    }

    usuario.alterarSenha(passwordEncoder.encode(request.novaSenha()));
    usuarioRepository.save(usuario);

    return emitirTokenPosAutenticacao(usuario);
  }

  @Transactional
  public SwitchTenantResponse trocarTenant(SwitchTenantRequest request) {
    UUID usuarioId = usuarioAutenticadoId();

    Usuario usuario =
        usuarioRepository.findById(usuarioId).orElseThrow(LoginCredenciaisInvalidasException::new);

    if (usuario.isDeveTrocarSenha()) {
      throw new SemAcessoEmpresaException();
    }

    UsuarioEmpresa vinculo =
        vinculoService
            .buscarVinculoAtivo(usuarioId, request.empresaId())
            .orElseThrow(SemAcessoEmpresaException::new);

    usuario.registrarNovoVinculo(request.empresaId());
    usuarioRepository.save(usuario);

    String token =
        jwtService.emitirToken(
            usuarioId.toString(), List.of(vinculo.getPerfil()), request.empresaId());

    return new SwitchTenantResponse(token);
  }

  private LoginResponse emitirTokenPosAutenticacao(Usuario usuario) {
    List<UsuarioEmpresa> vinculos = buscarVinculosAtivos(usuario.getId());

    if (vinculos.isEmpty()) {
      throw new SemVinculoAtivoException();
    }

    UUID empresaPadrao = vinculoService.resolverEmpresaPadrao(usuario.getLastEmpresaId(), vinculos);
    UsuarioEmpresa vinculoPadrao = vinculoDoTenant(vinculos, empresaPadrao);

    usuario.registrarNovoVinculo(empresaPadrao);
    usuarioRepository.save(usuario);

    String token =
        jwtService.emitirToken(
            usuario.getId().toString(), List.of(vinculoPadrao.getPerfil()), empresaPadrao);

    return new LoginResponse(token, false, montarEmpresasVinculadas(vinculos));
  }

  private List<UsuarioEmpresa> buscarVinculosAtivos(UUID usuarioId) {
    List<UUID> empresasAtivas = empresaService.listarIdsEmpresasAtivas();
    return vinculoService.listarVinculosAtivosDoUsuario(usuarioId, empresasAtivas);
  }

  private List<EmpresaVinculadaResponse> montarEmpresasVinculadas(List<UsuarioEmpresa> vinculos) {
    return vinculos.stream()
        .map(
            vinculo -> {
              var empresa = empresaService.buscarPorId(vinculo.getId().empresaId());
              return new EmpresaVinculadaResponse(
                  empresa.getId(), empresa.getNome(), vinculo.getPerfil());
            })
        .toList();
  }

  private UsuarioEmpresa vinculoDoTenant(List<UsuarioEmpresa> vinculos, UUID empresaId) {
    return vinculos.stream()
        .filter(v -> v.getId().empresaId().equals(empresaId))
        .findFirst()
        .orElseThrow(SemVinculoAtivoException::new);
  }

  private UUID usuarioAutenticadoId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return UUID.fromString(jwtAuth.getToken().getSubject());
    }

    throw new LoginCredenciaisInvalidasException();
  }
}
