package com.felipeduan.atendimento.modules.conversas;

import static com.felipeduan.atendimento.support.ConversaHttpSupport.patchConversa;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.postConversa;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.postConversaComCorpo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversaAberturaIntegrationTest extends AbstractConversaIntegrationTest {

  @Test
  void deveAbrirConversa_quandoContatoNaoTemConversaAberta() throws Exception {
    UUID contatoNovo =
        TenantContext.withTenantId(
            cenario.empresaId(),
            () -> contatoService.localizarOuCriar("5586999900099", "Novo Cliente"));

    postConversa(mockMvc, cenario.tokenAdmin(), contatoNovo.toString())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.contatoId").value(contatoNovo.toString()))
        .andExpect(jsonPath("$.status").value("ABERTA"))
        .andExpect(jsonPath("$.conversaAnteriorId").isEmpty());
  }

  @Test
  void deveReaproveitarConversaAberta_quandoJaExiste() throws Exception {
    String json =
        postConversa(mockMvc, cenario.tokenAdmin(), cenario.contatoId().toString())
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String conversaId = JsonPath.read(json, "$.id");

    assertThat(conversaId).isEqualTo(cenario.conversaId().toString());
  }

  @Test
  void deveCriarNovaConversa_quandoUltimaEstaEncerrada() throws Exception {
    patchConversa(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "ENCERRAR")
        .andExpect(status().isOk());

    String json =
        postConversa(mockMvc, cenario.tokenAdmin(), cenario.contatoId().toString())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ABERTA"))
            .andExpect(jsonPath("$.conversaAnteriorId").value(cenario.conversaId().toString()))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat((String) JsonPath.read(json, "$.id")).isNotEqualTo(cenario.conversaId().toString());
  }

  @Test
  void deveRetornar404_quandoContatoNaoExiste() throws Exception {
    postConversa(mockMvc, cenario.tokenAdmin(), UUID.randomUUID().toString())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Contato não encontrado"));
  }

  @Test
  void deveRetornar400_quandoContatoIdAusente() throws Exception {
    postConversaComCorpo(mockMvc, cenario.tokenAdmin(), "{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Dados inválidos"));
  }
}
