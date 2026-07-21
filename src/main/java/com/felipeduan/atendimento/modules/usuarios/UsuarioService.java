package com.felipeduan.atendimento.modules.usuarios;

import com.felipeduan.atendimento.modules.usuarios.exception.EmailExistenteSenhaInvalidaException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

  private final UsuarioRepository repository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public Usuario resolverOuCriarAdminInicial(
      String nome, String email, String senhaTemporaria, UUID empresaId) {

    var existente = repository.findByEmail(email);
    if (existente.isPresent()) {
      return reutilizarContaExistente(existente.get(), senhaTemporaria, empresaId);
    }
    return criarContaNova(nome, email, senhaTemporaria, empresaId);
  }

  private Usuario reutilizarContaExistente(Usuario usuario, String senha, UUID empresaId) {
    if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
      throw new EmailExistenteSenhaInvalidaException();
    }
    usuario.registrarNovoVinculo(empresaId);
    return repository.save(usuario);
  }

  private Usuario criarContaNova(String nome, String email, String senha, UUID empresaId) {
    return repository.save(
        Usuario.criarComSenhaTemporaria(nome, email, passwordEncoder.encode(senha), empresaId));
  }
}
