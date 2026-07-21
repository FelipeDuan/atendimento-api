package com.felipeduan.atendimento.modules.platformadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.AbstractIntegrationTest;
import com.felipeduan.atendimento.shared.security.JwtService;
import com.felipeduan.atendimento.shared.security.Roles;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
class PlatformAdminAuthIntegrationTest extends AbstractIntegrationTest {

  private static final String LOGIN_PATH = "/auth/plataforma/login";
  private static final String HEALTH_PATH = "/actuator/health";
  private static final String ENDPOINT_PROTEGIDO_PATH = "/empresas";

  private static final String EMAIL = "admin-teste@plataforma.local";
  private static final String SENHA = "senha-forte-de-teste";
  private static final String NOME = "Admin Teste";

  @Autowired MockMvc mockMvc;
  @Autowired JwtDecoder jwtDecoder;
  @Autowired AdministradorPlataformaRepository repository;
  @Autowired PasswordEncoder passwordEncoder;

  private UUID administradorId;

  @BeforeEach
  void prepararDados() {
    repository.deleteAll();
    administradorId = salvarAdministrador(EMAIL, SENHA).getId();
  }

  @Test
  void deveRetornarAccessToken_quandoCredenciaisValidas() throws Exception {
    ResultActions respostaLogin = performLogin(EMAIL, SENHA);
    String token = extrairAccessToken(respostaLogin);

    assertTokenPlatformAdmin(token);
  }

  @Test
  void deveRetornar401_quandoSenhaInvalida() throws Exception {
    ResultActions resposta = performLogin(EMAIL, "errada");

    assertCredenciaisInvalidas(resposta)
        .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
  }

  @Test
  void deveRetornar401_quandoEmailInexistente() throws Exception {
    ResultActions resposta = performLogin("naoexiste@plataforma.local", "qualquer");

    assertCredenciaisInvalidas(resposta);
  }

  @Test
  void deveRetornarHealthPublico_semToken() throws Exception {
    assertHealthOk(performGetHealth());
  }

  @Test
  void deveAcessarEndpointProtegido_comTokenValido() throws Exception {
    String token = obterAccessToken();

    mockMvc
        .perform(get(ENDPOINT_PROTEGIDO_PATH).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveRetornar401_semToken() throws Exception {
    assertUnauthorized(mockMvc.perform(get(ENDPOINT_PROTEGIDO_PATH)));
  }

  @Test
  void deveRetornar401_comTokenInvalido() throws Exception {
    assertUnauthorized(
        mockMvc.perform(
            get(ENDPOINT_PROTEGIDO_PATH).header("Authorization", "Bearer token-invalido")));
  }

  @Test
  void deveRetornar400_quandoEmailInvalido() throws Exception {
    ResultActions resposta = performLogin("nao-e-email", SENHA);

    resposta.andExpect(status().isBadRequest());
  }

  @Test
  void deveRetornar400_quandoSenhaEmBranco() throws Exception {
    ResultActions resposta = performLogin(EMAIL, "");

    resposta.andExpect(status().isBadRequest());
  }

  private AdministradorPlataforma salvarAdministrador(String email, String senha) {
    var administrador = new AdministradorPlataforma(NOME, email, passwordEncoder.encode(senha));
    return repository.save(administrador);
  }

  private String corpoLogin(String email, String senha) {
    return """
                {"email":"%s","senha":"%s"}
                """
        .formatted(email, senha);
  }

  private ResultActions performLogin(String email, String senha) throws Exception {
    return mockMvc.perform(
        post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(corpoLogin(email, senha)));
  }

  private String obterAccessToken() throws Exception {
    ResultActions respostaLogin = performLogin(EMAIL, SENHA);
    return extrairAccessToken(respostaLogin);
  }

  private String extrairAccessToken(ResultActions respostaLogin) throws Exception {
    String corpo =
        respostaLogin.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    return JsonPath.read(corpo, "$.accessToken");
  }

  private ResultActions performGetHealth() throws Exception {
    return mockMvc.perform(get(HEALTH_PATH));
  }

  private void assertTokenPlatformAdmin(String token) {
    Jwt jwt = jwtDecoder.decode(token);

    String subject = jwt.getSubject();
    List<String> authorities = jwt.getClaimAsStringList(JwtService.CLAIM_AUTHORITIES);
    boolean possuiTenantId = jwt.hasClaim(JwtService.CLAIM_TENANT_ID);

    assertThat(subject).isEqualTo(administradorId.toString());
    assertThat(authorities).containsExactly(Roles.PLATFORM_ADMIN);
    assertThat(possuiTenantId).isFalse();
    assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
  }

  private ResultActions assertHealthOk(ResultActions resposta) throws Exception {
    return resposta.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
  }

  private ResultActions assertUnauthorized(ResultActions resposta) throws Exception {
    return resposta.andExpect(status().isUnauthorized());
  }

  private ResultActions assertCredenciaisInvalidas(ResultActions resposta) throws Exception {
    return resposta
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Credenciais Inválidas"));
  }
}
