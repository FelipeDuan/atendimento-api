package com.felipeduan.atendimento.modules.conversas;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.getConversa;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.getMensagem;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.getMensagens;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.postMensagem;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.cnpjUnico;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoCriarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.postCriarEmpresa;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.EMAIL;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.SENHA;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import com.felipeduan.atendimento.support.PlatformAdminTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversaIsolamentoIntegrationTest extends AbstractConversaIntegrationTest {

  @Test
  void naoDeveVisualizarConversaDeOutroTenant() throws Exception {
    TenantB tenantB = criarTenantB();

    getConversa(mockMvc, cenario.tokenAdmin(), tenantB.conversaId().toString())
        .andExpect(status().isNotFound());

    getConversa(mockMvc, tenantB.tokenAdmin(), tenantB.conversaId().toString())
        .andExpect(status().isOk());
  }

  @Test
  void naoDeveAcessarMensagensDeOutroTenant() throws Exception {
    String jsonMensagem =
        postMensagem(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "só A")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String mensagemId = JsonPath.read(jsonMensagem, "$.id");

    TenantB tenantB = criarTenantB();

    getMensagem(mockMvc, tenantB.tokenAdmin(), mensagemId).andExpect(status().isNotFound());
    getMensagens(mockMvc, tenantB.tokenAdmin(), cenario.conversaId().toString())
        .andExpect(status().isNotFound());
    postMensagem(mockMvc, tenantB.tokenAdmin(), cenario.conversaId().toString(), "hack")
        .andExpect(status().isNotFound());
  }

  private TenantB criarTenantB() throws Exception {
    String cnpj = cnpjUnico();
    String emailAdminB = "admin-b-" + cnpj + "@empresa.local";
    String senhaAdminB = "SenhaTemp123!";
    String tokenPlatform = PlatformAdminTestSupport.obterToken(mockMvc, EMAIL, SENHA);

    String jsonEmpresaB =
        postCriarEmpresa(mockMvc, tokenPlatform, corpoCriarEmpresa(cnpj, emailAdminB, senhaAdminB))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID empresaB = UUID.fromString(JsonPath.read(jsonEmpresaB, "$.id"));
    liberarSenhaDefinitiva(usuarioRepository, passwordEncoder, emailAdminB, senhaAdminB);
    String tokenAdminB = obterToken(mockMvc, emailAdminB, senhaAdminB);

    UUID contatoB =
        TenantContext.withTenantId(
            empresaB, () -> contatoService.localizarOuCriar("5586999900002", "Cliente B"));
    UUID conversaB =
        TenantContext.withTenantId(
            empresaB, () -> conversaService.garantirConversaAberta(contatoB));

    return new TenantB(tokenAdminB, conversaB);
  }

  private record TenantB(String tokenAdmin, UUID conversaId) {}
}
