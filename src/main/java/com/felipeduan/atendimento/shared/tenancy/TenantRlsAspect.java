package com.felipeduan.atendimento.shared.tenancy;

import java.util.UUID;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@Aspect
@Component
@Order(1)
public class TenantRlsAspect {
    
    private final EntityManager entityManager;

    public TenantRlsAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("@annotation(transactional) || @within(transactional)")
    public void aplicarTenantNaSessao(Transactional transactional) {
        TenantContext.getTenantId().ifPresent(this::definirValorRls);
    }

    private void definirValorRls(UUID tenantId) {
        entityManager.createNativeQuery(
                "SELECT set_config('app.tenant_id', :tenantId, true)")
            .setParameter("tenantId", tenantId.toString())
            .getSingleResult();
    }   
}
