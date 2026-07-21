package com.felipeduan.atendimento.shared.tenancy;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TenantRlsConfigurer {

  private final EntityManager entityManager;

  public TenantRlsConfigurer(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public void aplicar(UUID tenantId) {
    entityManager
        .createNativeQuery("SELECT set_config('app.tenant_id', :tenantId, true)")
        .setParameter("tenantId", tenantId.toString())
        .getSingleResult();
  }
}
