package com.felipeduan.atendimento.support;

import com.felipeduan.atendimento.modules.usuarios.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

public final class AuthIntegrationTestSupport {

  public record CenarioAdminInicial(String emailAdmin, String senhaAdmin, UUID empresaId) {}

  private AuthIntegrationTestSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static void liberarSenhaDefinitiva(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      String email,
      String senha) {

    var usuario = usuarioRepository.findByEmail(email).orElseThrow();
    usuario.alterarSenha(passwordEncoder.encode(senha));
    usuarioRepository.save(usuario);
  }

  public static void inativarEmpresa(
      EntityManager entityManager, TransactionTemplate transactionTemplate, UUID empresaId) {

    transactionTemplate.executeWithoutResult(
        status ->
            entityManager
                .createNativeQuery("UPDATE empresa SET status = 'INATIVA' WHERE id = :empresaId")
                .setParameter("empresaId", empresaId)
                .executeUpdate());
  }
}
