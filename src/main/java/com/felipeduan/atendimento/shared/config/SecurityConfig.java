package com.felipeduan.atendimento.shared.config;

import com.felipeduan.atendimento.shared.security.JwtService;
import com.felipeduan.atendimento.shared.security.ProblemDetailAccessDeniedHandler;
import com.felipeduan.atendimento.shared.security.Roles;
import com.felipeduan.atendimento.shared.security.TenantContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final TenantContextFilter tenantContextFilter;
  private final ProblemDetailAccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(
      TenantContextFilter tenantContextFilter,
      ProblemDetailAccessDeniedHandler accessDeniedHandler) {
    this.tenantContextFilter = tenantContextFilter;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment)
      throws Exception {

    boolean documentacaoPublica = environment.matchesProfiles("dev");

    return baseChain(http)
        .authorizeHttpRequests(
            auth -> {
              regrasDeAutorizacao(auth);
              rotasPublicas(auth, documentacaoPublica);
              auth.anyRequest().access(negarTokenSomenteTrocarSenha());
            })
        .build();
  }

  private void regrasDeAutorizacao(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          auth) {

    auth.requestMatchers(HttpMethod.POST, "/empresas").hasAuthority(Roles.PLATFORM_ADMIN);
    auth.requestMatchers(HttpMethod.GET, "/empresas", "/empresas/inativas")
        .hasAuthority(Roles.PLATFORM_ADMIN);
    auth.requestMatchers(HttpMethod.DELETE, "/empresas/*").hasAuthority(Roles.PLATFORM_ADMIN);
    auth.requestMatchers(HttpMethod.POST, "/auth/trocar-senha").hasAuthority(Roles.TROCAR_SENHA);
    auth.requestMatchers("/conversas/**").hasAnyAuthority(Roles.ADMINISTRADOR, Roles.ATENDENTE);
    auth.requestMatchers("/mensagens/**").hasAnyAuthority(Roles.ADMINISTRADOR, Roles.ATENDENTE);
    auth.requestMatchers("/contatos/**").hasAnyAuthority(Roles.ADMINISTRADOR, Roles.ATENDENTE);
    auth.requestMatchers(HttpMethod.POST, "/usuarios").hasAuthority(Roles.ADMINISTRADOR);
    auth.requestMatchers(HttpMethod.PUT, "/usuarios/*").hasAuthority(Roles.ADMINISTRADOR);
    auth.requestMatchers(HttpMethod.GET, "/usuarios", "/usuarios/*")
        .hasAnyAuthority(Roles.ADMINISTRADOR, Roles.ATENDENTE);
  }

  private void rotasPublicas(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
      boolean incluirDocumentacao) {

    auth.requestMatchers("/auth/login", "/auth/plataforma/login", "/actuator/health").permitAll();
    auth.requestMatchers("/webhooks/**").permitAll();

    if (incluirDocumentacao) {
      auth.requestMatchers("/v3/api-docs/**", "/scalar", "/scalar/**").permitAll();
    }
  }

  private HttpSecurity baseChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .exceptionHandling(handling -> handling.accessDeniedHandler(this.accessDeniedHandler))
        .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class);
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName(JwtService.CLAIM_AUTHORITIES);
    authoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }

  private AuthorizationManager<RequestAuthorizationContext> negarTokenSomenteTrocarSenha() {
    return (authentication, context) -> {
      if (authentication.get() == null || !authentication.get().isAuthenticated()) {
        return new AuthorizationDecision(false);
      }

      boolean somenteTrocarSenha = true;
      for (GrantedAuthority authority : authentication.get().getAuthorities()) {
        if (!Roles.TROCAR_SENHA.equals(authority.getAuthority())) {
          somenteTrocarSenha = false;
          break;
        }
      }

      return new AuthorizationDecision(!somenteTrocarSenha);
    };
  }
}
