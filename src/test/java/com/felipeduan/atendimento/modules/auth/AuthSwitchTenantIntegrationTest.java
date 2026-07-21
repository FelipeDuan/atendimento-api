package com.felipeduan.atendimento.modules.auth;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthHttpSupport.obterTokenRestrito;
import static com.felipeduan.atendimento.support.AuthHttpSupport.postSwitchTenant;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.cnpjUnico;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoCriarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.postCriarEmpresa;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.EMAIL;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.SENHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.shared.security.JwtService;
import com.felipeduan.atendimento.support.PlatformAdminTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthSwitchTenantIntegrationTest extends AbstractAuthIntegrationTest {

  @Test
  void deveRetornar403_quandoTokenRestritoAcessaSwitchTenant() throws Exception {
    String tokenRestrito = obterTokenRestrito(mockMvc, cenario.emailAdmin(), cenario.senhaAdmin());

    postSwitchTenant(mockMvc, tokenRestrito, cenario.empresaId()).andExpect(status().isForbidden());
  }

  @Test
  void deveRetornar403_quandoSwitchParaEmpresaSemVinculo() throws Exception {
    liberarSenhaDefinitiva(
        usuarioRepository, passwordEncoder, cenario.emailAdmin(), cenario.senhaAdmin());

    String tokenLogin = obterToken(mockMvc, cenario.emailAdmin(), cenario.senhaAdmin());
    UUID empresaSemVinculo = UUID.randomUUID();

    postSwitchTenant(mockMvc, tokenLogin, empresaSemVinculo)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Acesso negado"));
  }

  @Test
  void deveTrocarTenant_quandoUsuarioTemDoisVinculos() throws Exception {
    String cnpj2 = cnpjUnico();
    String tokenPlatform = PlatformAdminTestSupport.obterToken(mockMvc, EMAIL, SENHA);
    String jsonSegunda =
        postCriarEmpresa(
                mockMvc,
                tokenPlatform,
                corpoCriarEmpresa(cnpj2, cenario.emailAdmin(), cenario.senhaAdmin()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID empresaId2 = UUID.fromString(JsonPath.read(jsonSegunda, "$.id"));

    var usuario = usuarioRepository.findByEmail(cenario.emailAdmin()).orElseThrow();

    usuario.alterarSenha(passwordEncoder.encode(cenario.senhaAdmin()));
    usuario.registrarNovoVinculo(cenario.empresaId());
    usuarioRepository.save(usuario);

    String tokenLogin = obterToken(mockMvc, cenario.emailAdmin(), cenario.senhaAdmin());
    UUID tenantLogin =
        UUID.fromString(jwtDecoder.decode(tokenLogin).getClaimAsString(JwtService.CLAIM_TENANT_ID));

    assertThat(tenantLogin).isEqualTo(cenario.empresaId());

    String jsonSwitch =
        postSwitchTenant(mockMvc, tokenLogin, empresaId2)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID tenantNovo =
        UUID.fromString(
            jwtDecoder
                .decode(JsonPath.read(jsonSwitch, "$.accessToken"))
                .getClaimAsString(JwtService.CLAIM_TENANT_ID));

    assertThat(tenantNovo).isEqualTo(empresaId2);
    assertThat(tenantNovo).isNotEqualTo(tenantLogin);
  }
}
