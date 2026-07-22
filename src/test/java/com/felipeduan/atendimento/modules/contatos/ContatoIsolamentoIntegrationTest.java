package com.felipeduan.atendimento.modules.contatos;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static com.felipeduan.atendimento.support.ContatoHttpSupport.corpoCriarContato;
import static com.felipeduan.atendimento.support.ContatoHttpSupport.getContato;
import static com.felipeduan.atendimento.support.ContatoHttpSupport.getContatos;
import static com.felipeduan.atendimento.support.ContatoHttpSupport.postContato;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.cnpjUnico;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoCriarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.postCriarEmpresa;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.EMAIL;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.SENHA;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.modules.conversas.AbstractConversaIntegrationTest;
import com.felipeduan.atendimento.support.PlatformAdminTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class ContatoIsolamentoIntegrationTest extends AbstractConversaIntegrationTest {

  @Test
  void naoDeveVisualizarContatoDeOutroTenant() throws Exception {
    String json =
        postContato(
                mockMvc, cenario.tokenAdmin(), corpoCriarContato("Só Tenant A", "5586999955555"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String contatoA = JsonPath.read(json, "$.id");

    String tokenB = criarTokenTenantB();

    getContato(mockMvc, tokenB, contatoA).andExpect(status().isNotFound());

    String listagem =
        getContatos(mockMvc, tokenB)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.assertj.core.api.Assertions.assertThat(listagem).doesNotContain(contatoA);
  }

  @Test
  void devePermitirMesmoNumeroEmEmpresasDiferentes() throws Exception {
    String numero = "5586999966666";
    postContato(mockMvc, cenario.tokenAdmin(), corpoCriarContato("Na A", numero))
        .andExpect(status().isCreated());

    String tokenB = criarTokenTenantB();
    postContato(mockMvc, tokenB, corpoCriarContato("Na B", numero))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.numeroWhatsapp").value(numero));
  }

  private String criarTokenTenantB() throws Exception {
    String cnpj = cnpjUnico();
    String emailAdminB = "admin-contato-b-" + cnpj + "@empresa.local";
    String senhaAdminB = "SenhaTemp123!";
    String tokenPlatform = PlatformAdminTestSupport.obterToken(mockMvc, EMAIL, SENHA);

    postCriarEmpresa(mockMvc, tokenPlatform, corpoCriarEmpresa(cnpj, emailAdminB, senhaAdminB))
        .andExpect(status().isCreated());

    liberarSenhaDefinitiva(usuarioRepository, passwordEncoder, emailAdminB, senhaAdminB);
    return obterToken(mockMvc, emailAdminB, senhaAdminB);
  }
}
