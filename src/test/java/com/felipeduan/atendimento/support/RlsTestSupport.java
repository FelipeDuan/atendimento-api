package com.felipeduan.atendimento.support;

import com.felipeduan.atendimento.modules.vinculos.UsuarioEmpresaRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.transaction.support.TransactionTemplate;

public final class RlsTestSupport {

  private RlsTestSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static long contarVinculosDaEmpresa(
      UUID empresaId,
      UsuarioEmpresaRepository repository,
      EntityManager entityManager,
      TransactionTemplate transactionTemplate) {

    return transactionTemplate.execute(
        status -> {
          definirTenant(entityManager, empresaId);
          return repository.count();
        });
  }

  static void definirTenant(EntityManager entityManager, UUID empresaId) {
    entityManager
        .createNativeQuery("SELECT set_config('app.tenant_id', :tenantId, true)")
        .setParameter("tenantId", empresaId.toString())
        .getSingleResult();
  }
}
