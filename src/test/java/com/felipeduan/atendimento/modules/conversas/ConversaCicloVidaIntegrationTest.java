package com.felipeduan.atendimento.modules.conversas;

import static com.felipeduan.atendimento.support.ConversaHttpSupport.getMensagem;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.getMensagens;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.getMensagensSemFiltro;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.patchConversa;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.postMensagem;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.postMensagemComCorpo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversaCicloVidaIntegrationTest extends AbstractConversaIntegrationTest {

  @Test
  void deveRegistrarMensagem_quandoConversaAberta() throws Exception {
    postMensagem(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "Olá cliente")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.conteudo").value("Olá cliente"))
        .andExpect(jsonPath("$.sentido").value("SAIDA"))
        .andExpect(jsonPath("$.envioPendente").value(true));
  }

  @Test
  void deveRetornar409_quandoRegistraMensagemEmConversaEncerrada() throws Exception {
    patchConversa(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "ENCERRAR")
        .andExpect(status().isOk());

    postMensagem(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "depois")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Conversa encerrada"));
  }

  @Test
  void deveEncerrarEReabrirConversa() throws Exception {
    patchConversa(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "ENCERRAR")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ENCERRADA"));

    patchConversa(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "REABRIR")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ABERTA"))
        .andExpect(jsonPath("$.dataEncerramento").isEmpty());
  }

  @Test
  void deveRetornar409_quandoEncerraConversaJaEncerrada() throws Exception {
    patchConversa(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "ENCERRAR")
        .andExpect(status().isOk());

    patchConversa(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "ENCERRAR")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Estado inválido"));
  }

  @Test
  void deveListarMensagensEmOrdemCronologica() throws Exception {
    postMensagem(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "primeira")
        .andExpect(status().isCreated());
    postMensagem(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "segunda")
        .andExpect(status().isCreated());

    getMensagens(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].conteudo").value("primeira"))
        .andExpect(jsonPath("$.content[1].conteudo").value("segunda"));
  }

  @Test
  void deveBuscarMensagemPorId() throws Exception {
    String json =
        postMensagem(mockMvc, cenario.tokenAdmin(), cenario.conversaId().toString(), "única")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String mensagemId = JsonPath.read(json, "$.id");

    getMensagem(mockMvc, cenario.tokenAdmin(), mensagemId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(mensagemId))
        .andExpect(jsonPath("$.conteudo").value("única"))
        .andExpect(jsonPath("$.conversaId").value(cenario.conversaId().toString()));
  }

  @Test
  void deveRetornar400_quandoEnviaMensagemSemConversaId() throws Exception {
    postMensagemComCorpo(mockMvc, cenario.tokenAdmin(), "{\"tipo\":\"TEXTO\",\"conteudo\":\"oi\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Dados inválidos"));
  }

  @Test
  void deveRetornar404_quandoMensagemNaoExiste() throws Exception {
    getMensagem(mockMvc, cenario.tokenAdmin(), UUID.randomUUID().toString())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Mensagem não encontrada"));
  }

  @Test
  void deveRetornar400_quandoListaMensagensSemConversaId() throws Exception {
    getMensagensSemFiltro(mockMvc, cenario.tokenAdmin()).andExpect(status().isBadRequest());
  }
}
