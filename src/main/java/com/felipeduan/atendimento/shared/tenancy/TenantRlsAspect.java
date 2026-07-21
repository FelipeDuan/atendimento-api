package com.felipeduan.atendimento.shared.tenancy;

import java.util.UUID;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
@Order(1)
public class TenantRlsAspect {

  private final TenantRlsConfigurer tenantRlsConfigurer;

  public TenantRlsAspect(TenantRlsConfigurer tenantRlsConfigurer) {
    this.tenantRlsConfigurer = tenantRlsConfigurer;
  }

  @Before("@annotation(transactional) || @within(transactional)")
  public void aplicarTenantNaSessao(Transactional transactional) {
    TenantContext.getTenantId().ifPresent(this::definirValorRls);
  }

  private void definirValorRls(UUID tenantId) {
    tenantRlsConfigurer.aplicar(tenantId);
  }
}
