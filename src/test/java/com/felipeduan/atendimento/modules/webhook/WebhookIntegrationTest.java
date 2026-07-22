package com.felipeduan.atendimento.modules.webhook;

import static com.felipeduan.atendimento.support.ContatoHttpSupport.getContatos;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.getConversas;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.getMensagens;
import static com.felipeduan.atendimento.support.ConversaHttpSupport.patchConversa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoAtualizarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.putEmpresa;
import static com.felipeduan.atendimento.support.WebhookHttpSupport.corpoMensagemImagem;
import static com.felipeduan.atendimento.support.WebhookHttpSupport.corpoMensagemTexto;
import static com.felipeduan.atendimento.support.WebhookHttpSupport.getVerificacao;
import static com.felipeduan.atendimento.support.WebhookHttpSupport.postWebhook;
import static com.felipeduan.atendimento.support.WebhookHttpSupport.postWebhookComAssinatura;
import static com.felipeduan.atendimento.support.WebhookHttpSupport.postWebhookSemAssinatura;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.modules.conversas.AbstractConversaIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WebhookIntegrationTest extends AbstractConversaIntegrationTest {

  private static final String PHONE_NUMBER_ID = "phone-test-webhook-001";

  @Autowired private AssinaturaWebhook assinaturaWebhook;

  @BeforeEach
  void configurarPhoneNumberId() throws Exception {
    putEmpresa(
            mockMvc,
            cenario.tokenAdmin(),
            cenario.empresaId(),
            corpoAtualizarEmpresa("Empresa Webhook", "admin@empresa.local", PHONE_NUMBER_ID))
        .andExpect(status().isOk());
  }

  @Test
  void deveRetornar401_quandoAssinaturaInvalida() throws Exception {
    String corpo =
        corpoMensagemTexto(PHONE_NUMBER_ID, "5586999977777", "Cliente", "wamid.bad", "oi");

    postWebhookComAssinatura(mockMvc, "sha256=00", corpo)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Assinatura inválida"));
  }

  @Test
  void deveRetornar401_quandoAssinaturaAusente() throws Exception {
    String corpo =
        corpoMensagemTexto(PHONE_NUMBER_ID, "5586999977777", "Cliente", "wamid.missing", "oi");

    postWebhookSemAssinatura(mockMvc, corpo)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Assinatura inválida"));
  }

  @Test
  void deveResponderChallenge_quandoTokenCorreto() throws Exception {
    getVerificacao(mockMvc, "subscribe", "test-verify-token", "desafio-123")
        .andExpect(status().isOk())
        .andExpect(content().string("desafio-123"));
  }

  @Test
  void deveRetornar403_quandoTokenIncorreto() throws Exception {
    getVerificacao(mockMvc, "subscribe", "token-errado", "desafio-123")
        .andExpect(status().isForbidden());
  }

  @Test
  void deveCriarContatoConversaEMensagem_quandoMensagemNova() throws Exception {
    String waId = "5586999988888";
    String corpo =
        corpoMensagemTexto(PHONE_NUMBER_ID, waId, "Cliente Webhook", "wamid.novo.1", "Olá time");

    postWebhook(mockMvc, assinaturaWebhook, corpo).andExpect(status().isOk());

    String contatoId = contatoIdPorNumero(waId);
    String conversaId = conversaIdPorContato(contatoId);

    getMensagens(mockMvc, cenario.tokenAdmin(), conversaId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].conteudo").value("Olá time"))
        .andExpect(jsonPath("$.content[0].sentido").value("ENTRADA"));
  }

  @Test
  void naoDeveDuplicarMensagem_quandoMetaReentrega() throws Exception {
    String waId = "5586999988889";
    String corpo = corpoMensagemTexto(PHONE_NUMBER_ID, waId, "Cliente Dup", "wamid.dup.1", "mesma");

    postWebhook(mockMvc, assinaturaWebhook, corpo).andExpect(status().isOk());
    postWebhook(mockMvc, assinaturaWebhook, corpo).andExpect(status().isOk());

    String conversaId = conversaIdPorContato(contatoIdPorNumero(waId));
    getMensagens(mockMvc, cenario.tokenAdmin(), conversaId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1));
  }

  @Test
  void deveCriarNovaConversaComReferencia_quandoUltimaEncerrada() throws Exception {
    String waId = "5586999988890";
    String corpo1 =
        corpoMensagemTexto(PHONE_NUMBER_ID, waId, "Cliente Cont", "wamid.cont.1", "primeira");
    postWebhook(mockMvc, assinaturaWebhook, corpo1).andExpect(status().isOk());

    String contatoId = contatoIdPorNumero(waId);
    String conversaAntiga = conversaIdPorContato(contatoId);

    patchConversa(mockMvc, cenario.tokenAdmin(), conversaAntiga, "ENCERRAR")
        .andExpect(status().isOk());

    String corpo2 =
        corpoMensagemTexto(PHONE_NUMBER_ID, waId, "Cliente Cont", "wamid.cont.2", "segunda");
    postWebhook(mockMvc, assinaturaWebhook, corpo2).andExpect(status().isOk());

    String conversas =
        getConversas(mockMvc, cenario.tokenAdmin())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    List<Map<String, Object>> abertas =
        JsonPath.read(
            conversas,
            "$.content[?(@.status == 'ABERTA' && @.conversaAnteriorId == '%s')]"
                .formatted(conversaAntiga));
    assertThat(abertas).hasSize(1);
  }

  @Test
  void deveRegistrarImagemComoPlaceholder_quandoTipoImage() throws Exception {
    String waId = "5586999988892";
    String corpo =
        corpoMensagemImagem(PHONE_NUMBER_ID, waId, "Cliente Foto", "wamid.img.1", "foto da fatura");

    postWebhook(mockMvc, assinaturaWebhook, corpo).andExpect(status().isOk());

    String conversaId = conversaIdPorContato(contatoIdPorNumero(waId));
    getMensagens(mockMvc, cenario.tokenAdmin(), conversaId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].tipo").value("IMAGEM"))
        .andExpect(jsonPath("$.content[0].conteudo").value("foto da fatura"))
        .andExpect(jsonPath("$.content[0].sentido").value("ENTRADA"));
  }

  @Test
  void deveIgnorarPayload_quandoPhoneNumberIdDesconhecido() throws Exception {
    String corpo =
        corpoMensagemTexto(
            "phone-desconhecido", "5586999988891", "Ghost", "wamid.ghost.1", "sumiu");

    postWebhook(mockMvc, assinaturaWebhook, corpo).andExpect(status().isOk());

    String listagem =
        getContatos(mockMvc, cenario.tokenAdmin())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(listagem).doesNotContain("5586999988891");
  }

  private String contatoIdPorNumero(String waId) throws Exception {
    String contatos =
        getContatos(mockMvc, cenario.tokenAdmin())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    List<Map<String, Object>> matches =
        JsonPath.read(contatos, "$.content[?(@.numeroWhatsapp == '%s')]".formatted(waId));
    assertThat(matches).isNotEmpty();
    return matches.getFirst().get("id").toString();
  }

  private String conversaIdPorContato(String contatoId) throws Exception {
    String conversas =
        getConversas(mockMvc, cenario.tokenAdmin())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    List<Map<String, Object>> matches =
        JsonPath.read(conversas, "$.content[?(@.contatoId == '%s')]".formatted(contatoId));
    assertThat(matches).isNotEmpty();
    return matches.getFirst().get("id").toString();
  }
}
