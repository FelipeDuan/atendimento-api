package com.felipeduan.atendimento.support;

import com.felipeduan.atendimento.modules.empresas.Empresa;
import com.felipeduan.atendimento.modules.empresas.EmpresaRepository;
import com.felipeduan.atendimento.modules.usuarios.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.transaction.support.TransactionTemplate;

public final class LimpezaDadosTestSupport {

  private LimpezaDadosTestSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static void limparDadosNegocio(
      EmpresaRepository empresaRepository,
      UsuarioRepository usuarioRepository,
      EntityManager entityManager,
      TransactionTemplate transactionTemplate) {

    for (Empresa empresa : empresaRepository.findAll()) {
      UUID empresaId = empresa.getId();
      transactionTemplate.executeWithoutResult(
          status -> {
            RlsTestSupport.definirTenant(entityManager, empresaId);
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
          empresaRepository.deleteAllInBatch();
        });
  }
}
