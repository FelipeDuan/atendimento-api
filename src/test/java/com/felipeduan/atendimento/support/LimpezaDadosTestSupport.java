package com.felipeduan.atendimento.support;

import com.felipeduan.atendimento.modules.usuarios.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.support.TransactionTemplate;

public final class LimpezaDadosTestSupport {

  private LimpezaDadosTestSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static void limparDadosNegocio(
      UsuarioRepository usuarioRepository,
      EntityManager entityManager,
      TransactionTemplate transactionTemplate) {

    @SuppressWarnings("unchecked")
    List<Object> idsEmpresa =
        entityManager.createNativeQuery("SELECT id FROM empresa").getResultList();

    for (Object rawId : idsEmpresa) {
      UUID empresaId = rawId instanceof UUID uuid ? uuid : UUID.fromString(rawId.toString());
      transactionTemplate.executeWithoutResult(
          status -> {
            RlsTestSupport.definirTenant(entityManager, empresaId);
            entityManager
                .createNativeQuery("DELETE FROM mensagem WHERE empresa_id = :empresaId")
                .setParameter("empresaId", empresaId)
                .executeUpdate();
            entityManager
                .createNativeQuery("DELETE FROM conversa WHERE empresa_id = :empresaId")
                .setParameter("empresaId", empresaId)
                .executeUpdate();
            entityManager
                .createNativeQuery("DELETE FROM contato WHERE empresa_id = :empresaId")
                .setParameter("empresaId", empresaId)
                .executeUpdate();
            entityManager
                .createNativeQuery("DELETE FROM usuario_empresa WHERE empresa_id = :empresaId")
                .setParameter("empresaId", empresaId)
                .executeUpdate();
          });
    }

    transactionTemplate.executeWithoutResult(
        status -> {
          entityManager
              .createNativeQuery(
                  "UPDATE usuario SET last_empresa_id = NULL WHERE last_empresa_id IS NOT NULL")
              .executeUpdate();
          usuarioRepository.deleteAllInBatch();
          entityManager.createNativeQuery("DELETE FROM empresa").executeUpdate();
        });
  }
}
