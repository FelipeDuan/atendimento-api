package com.felipeduan.atendimento.modules.auth;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthHttpSupport.obterTokenRestrito;
import static com.felipeduan.atendimento.support.AuthHttpSupport.postTrocarSenha;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.inativarEmpresa;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.shared.security.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class AuthTrocarSenhaIntegrationTest extends AbstractAuthIntegrationTest {

  @Test
  void deveEmitirTokenComTenant_aposTrocarSenha() throws Exception {
    String tokenRestrito = obterTokenRestrito(mockMvc, cenario.emailAdmin(), cenario.senhaAdmin());
    String novaSenha = "NovaSenha456!";

    String json =
        postTrocarSenha(mockMvc, tokenRestrito, cenario.senhaAdmin(), novaSenha)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exigeTrocarSenha").value(false))
            .andExpect(jsonPath("$.empresasVinculadas.length()").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();

    var jwt = jwtDecoder.decode(JsonPath.read(json, "$.accessToken"));

    assertThat(jwt.getClaimAsStringList(JwtService.CLAIM_AUTHORITIES))
        .containsExactly("ADMINISTRADOR");
    assertThat(jwt.getClaimAsString(JwtService.CLAIM_TENANT_ID))
        .isEqualTo(cenario.empresaId().toString());

    String tokenPosTroca = obterToken(mockMvc, cenario.emailAdmin(), novaSenha);
    assertThat(jwtDecoder.decode(tokenPosTroca).hasClaim(JwtService.CLAIM_TENANT_ID)).isTrue();
  }

  @Test
  void deveRetornar401_quandoSenhaAtualInvalidaNoTrocarSenha() throws Exception {
    String tokenRestrito = obterTokenRestrito(mockMvc, cenario.emailAdmin(), cenario.senhaAdmin());

    postTrocarSenha(mockMvc, tokenRestrito, "errada", "NovaSenha456!")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Credenciais Inválidas"));
  }

  @Test
  void deveRetornar403_quandoTokenNormalAcessaTrocarSenha() throws Exception {
    liberarSenhaDefinitiva(
        usuarioRepository, passwordEncoder, cenario.emailAdmin(), cenario.senhaAdmin());

    String tokenNormal = obterToken(mockMvc, cenario.emailAdmin(), cenario.senhaAdmin());

    postTrocarSenha(mockMvc, tokenNormal, cenario.senhaAdmin(), "OutraSenha789!")
        .andExpect(status().isForbidden());
  }

  @Test
  void deveRetornar403_quandoSemVinculoAtivoNoTrocarSenha() throws Exception {
    inativarEmpresa(entityManager, transactionTemplate, cenario.empresaId());

    String tokenRestrito = obterTokenRestrito(mockMvc, cenario.emailAdmin(), cenario.senhaAdmin());

    postTrocarSenha(mockMvc, tokenRestrito, cenario.senhaAdmin(), "NovaSenha456!")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Sem vínculo ativo"));
  }
}
