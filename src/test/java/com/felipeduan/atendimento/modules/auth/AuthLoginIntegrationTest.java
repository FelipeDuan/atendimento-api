package com.felipeduan.atendimento.modules.auth;

import static com.felipeduan.atendimento.support.AuthHttpSupport.postLogin;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.inativarEmpresa;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.shared.security.JwtService;
import com.felipeduan.atendimento.shared.security.Roles;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class AuthLoginIntegrationTest extends AbstractAuthIntegrationTest {

  @Test
  void deveEmitirTokenTrocarSenha_quandoAdminInicial() throws Exception {
    String json =
        postLogin(mockMvc, cenario.emailAdmin(), cenario.senhaAdmin())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exigeTrocarSenha").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

    var jwt = jwtDecoder.decode(JsonPath.read(json, "$.accessToken"));

    assertThat(jwt.getClaimAsStringList(JwtService.CLAIM_AUTHORITIES))
        .containsExactly(Roles.TROCAR_SENHA);
    assertThat(jwt.hasClaim(JwtService.CLAIM_TENANT_ID)).isFalse();
  }

  @Test
  void deveRetornar401_quandoSenhaInvalida() throws Exception {
    postLogin(mockMvc, cenario.emailAdmin(), "errada")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Credenciais Inválidas"));
  }

  @Test
  void deveRetornar403_quandoSemVinculoAtivoNoLogin() throws Exception {
    liberarSenhaDefinitiva(
        usuarioRepository, passwordEncoder, cenario.emailAdmin(), cenario.senhaAdmin());
    inativarEmpresa(entityManager, transactionTemplate, cenario.empresaId());

    postLogin(mockMvc, cenario.emailAdmin(), cenario.senhaAdmin())
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Sem vínculo ativo"));
  }
}
