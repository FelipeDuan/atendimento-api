package com.felipeduan.atendimento.modules.mensagens;

import static com.felipeduan.atendimento.support.ConversaHttpSupport.getMensagens;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.patchConversa;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.postMensagemEntrada;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.modules.conversas.AbstractConversaIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class MensagemEntradaIntegrationTest extends AbstractConversaIntegrationTest {

  @Test
  void deveSimularEntrada_criandoMensagemNoTenant() throws Exception {
    postMensagemEntrada(
            mockMvc,
            cenario.tokenAdmin(),
            "5586999912345",
            "Cliente Demo",
            "Oi pelo Scalar",
            "sim-wamid-1")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sentido").value("ENTRADA"))
        .andExpect(jsonPath("$.conteudo").value("Oi pelo Scalar"))
        .andExpect(jsonPath("$.whatsappMessageId").value("sim-wamid-1"))
        .andExpect(jsonPath("$.envioPendente").value(false));
  }

  @Test
  void deveRetornarMesmaMensagem_quandoWhatsappMessageIdRepetido() throws Exception {
    String primeira =
        postMensagemEntrada(
                mockMvc,
                cenario.tokenAdmin(),
                "5586999912345",
                "Cliente Demo",
                "primeira",
                "sim-wamid-dup")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id = JsonPath.read(primeira, "$.id");

    postMensagemEntrada(
            mockMvc,
            cenario.tokenAdmin(),
            "5586999912345",
            "Cliente Demo",
            "segunda tentativa",
            "sim-wamid-dup")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.conteudo").value("primeira"));
  }

  @Test
  void deveAbrirNovaConversa_quandoUltimaEstaEncerrada() throws Exception {
    patchConversa(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "ENCERRAR")
        .andExpect(status().isOk());

    String json =
        postMensagemEntrada(
                mockMvc,
                cenario.tokenAdmin(),
                "5586999900001",
                "Cliente Teste",
                "depois do encerramento",
                "sim-wamid-nova-conv")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sentido").value("ENTRADA"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String novaConversaId = JsonPath.read(json, "$.conversaId");

    getMensagens(mockMvc, cenario.tokenAdmin(), novaConversaId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].conteudo").value("depois do encerramento"));
  }

  @Test
  void deveRetornar400_quandoNumeroAusente() throws Exception {
    postMensagemEntrada(
            mockMvc,
            cenario.tokenAdmin(),
            """
            {"tipo":"TEXTO","conteudo":"sem numero"}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Dados inválidos"));
  }
}
