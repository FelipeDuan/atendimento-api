package com.felipeduan.atendimento.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AtendimentoPermissionEvaluatorTest {

  @Mock JwtService jwtService;

  AtendimentoPermissionEvaluator evaluator;

  UUID empresaId;

  @BeforeEach
  void preparar() {
    evaluator = new AtendimentoPermissionEvaluator(jwtService);
    empresaId = UUID.randomUUID();
  }

  @Test
  void devePermitir_quandoPlatformAdmin() {
    var auth = autenticacao(Roles.PLATFORM_ADMIN, null);

    assertThat(evaluator.hasPermission(auth, empresaId, "Empresa", EmpresaPermissions.READ))
        .isTrue();
  }

  @Test
  void devePermitir_quandoAdministradorDoMesmoTenant() {
    var jwt = jwtComTenant(empresaId);
    var auth = autenticacao(Roles.ADMINISTRADOR, jwt);
    when(jwtService.lerTenantId(jwt)).thenReturn(Optional.of(empresaId));

    assertThat(evaluator.hasPermission(auth, empresaId, "Empresa", EmpresaPermissions.WRITE))
        .isTrue();
  }

  @Test
  void naoDevePermitir_quandoAdministradorDeOutroTenant() {
    var jwt = jwtComTenant(UUID.randomUUID());
    var auth = autenticacao(Roles.ADMINISTRADOR, jwt);
    when(jwtService.lerTenantId(jwt)).thenReturn(Optional.of(UUID.randomUUID()));

    assertThat(evaluator.hasPermission(auth, empresaId, "Empresa", EmpresaPermissions.READ))
        .isFalse();
  }

  @Test
  void naoDevePermitir_quandoAtendente() {
    var jwt = jwtComTenant(empresaId);
    var auth = autenticacao(Roles.ATENDENTE, jwt);

    assertThat(evaluator.hasPermission(auth, empresaId, "Empresa", EmpresaPermissions.READ))
        .isFalse();
  }

  private JwtAuthenticationToken autenticacao(String role, Jwt jwt) {
    if (jwt == null) {
      jwt =
          Jwt.withTokenValue("token")
              .header("alg", "none")
              .subject(UUID.randomUUID().toString())
              .issuedAt(Instant.now())
              .expiresAt(Instant.now().plusSeconds(60))
              .claim(JwtService.CLAIM_AUTHORITIES, List.of(role))
              .build();
    }

    return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role)));
  }

  private Jwt jwtComTenant(UUID tenantId) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(UUID.randomUUID().toString())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .claim(JwtService.CLAIM_AUTHORITIES, List.of(Roles.ADMINISTRADOR))
        .claim(JwtService.CLAIM_TENANT_ID, tenantId.toString())
        .build();
  }
}
