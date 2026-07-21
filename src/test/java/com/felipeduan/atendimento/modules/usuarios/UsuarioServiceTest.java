package com.felipeduan.atendimento.modules.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.felipeduan.atendimento.modules.usuarios.exception.EmailExistenteSenhaInvalidaException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

  private static final UUID EMPRESA_ID = UUID.randomUUID();

  @Mock UsuarioRepository repository;
  @Mock PasswordEncoder passwordEncoder;

  @InjectMocks UsuarioService service;

  @Test
  void deveCriarContaNova_quandoEmailInexistente() {
    when(repository.findByEmail("admin@empresa.local")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("SenhaTemp123!")).thenReturn("hash-encodado");
    when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Usuario usuario =
        service.resolverOuCriarAdminInicial(
            "Admin", "admin@empresa.local", "SenhaTemp123!", EMPRESA_ID);

    assertThat(usuario.getEmail()).isEqualTo("admin@empresa.local");
    assertThat(usuario.isDeveTrocarSenha()).isTrue();
    assertThat(usuario.getLastEmpresaId()).isEqualTo(EMPRESA_ID);
    verify(passwordEncoder).encode("SenhaTemp123!");
  }

  @Test
  void deveReutilizarConta_quandoEmailExisteESenhaCorreta() {
    var existente =
        Usuario.criarComSenhaTemporaria("Admin", "admin@empresa.local", "hash-antigo", EMPRESA_ID);
    UUID novaEmpresaId = UUID.randomUUID();

    when(repository.findByEmail("admin@empresa.local")).thenReturn(Optional.of(existente));
    when(passwordEncoder.matches("SenhaTemp123!", "hash-antigo")).thenReturn(true);
    when(repository.save(existente)).thenReturn(existente);

    Usuario usuario =
        service.resolverOuCriarAdminInicial(
            "Outro Nome", "admin@empresa.local", "SenhaTemp123!", novaEmpresaId);

    assertThat(usuario.getLastEmpresaId()).isEqualTo(novaEmpresaId);
    assertThat(usuario.isDeveTrocarSenha()).isTrue();
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  void deveLancarExcecao_quandoEmailExisteESenhaIncorreta() {
    var existente =
        Usuario.criarComSenhaTemporaria("Admin", "admin@empresa.local", "hash-antigo", EMPRESA_ID);

    when(repository.findByEmail("admin@empresa.local")).thenReturn(Optional.of(existente));
    when(passwordEncoder.matches("errada", "hash-antigo")).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.resolverOuCriarAdminInicial(
                    "Admin", "admin@empresa.local", "errada", UUID.randomUUID()))
        .isInstanceOf(EmailExistenteSenhaInvalidaException.class);

    verify(repository, never()).save(any());
  }
}
