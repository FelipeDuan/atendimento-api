package com.felipeduan.atendimento.shared.security;

import java.io.Serializable;
import java.util.UUID;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AtendimentoPermissionEvaluator implements PermissionEvaluator {

  private static final String TARGET_EMPRESA = "Empresa";

  private final JwtService jwtService;

  public AtendimentoPermissionEvaluator(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Object targetDomainObject, Object permission) {
    return false;
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {

    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    if (!TARGET_EMPRESA.equals(targetType) || !(targetId instanceof UUID empresaId)) {
      return false;
    }

    if (possuiAutoridade(authentication, Roles.PLATFORM_ADMIN)) {
      return true;
    }

    if (!possuiAutoridade(authentication, Roles.ADMINISTRADOR)) {
      return false;
    }

    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      return false;
    }

    return jwtService.lerTenantId(jwtAuth.getToken()).filter(empresaId::equals).isPresent();
  }

  private boolean possuiAutoridade(Authentication authentication, String autoridade) {
    return authentication.getAuthorities().stream()
        .anyMatch(granted -> autoridade.equals(granted.getAuthority()));
  }
}
