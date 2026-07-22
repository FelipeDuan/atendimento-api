package com.felipeduan.atendimento.modules.mensagens;

import static org.assertj.core.api.Assertions.assertThat;

import com.felipeduan.atendimento.modules.conversas.dto.ConversaParaMensagem;
import com.felipeduan.atendimento.modules.mensagens.enums.SentidoMensagem;
import com.felipeduan.atendimento.modules.mensagens.enums.TipoMensagem;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MensagemTest {

  private static final ConversaParaMensagem CONVERSA =
      new ConversaParaMensagem(UUID.randomUUID(), UUID.randomUUID());

  @Test
  void deveCriarMensagemDeSaidaPendente() {
    Mensagem mensagem = Mensagem.saida(CONVERSA, TipoMensagem.TEXTO, "oi");

    assertThat(mensagem.getSentido()).isEqualTo(SentidoMensagem.SAIDA);
    assertThat(mensagem.getWhatsappMessageId()).isNull();
    assertThat(mensagem.isEnvioPendente()).isTrue();
    assertThat(mensagem.getConversaId()).isEqualTo(CONVERSA.id());
    assertThat(mensagem.getEmpresaId()).isEqualTo(CONVERSA.empresaId());
  }

  @Test
  void deveCriarMensagemDeEntradaComWhatsappId() {
    Mensagem mensagem = Mensagem.entrada(CONVERSA, TipoMensagem.TEXTO, "oi", "wamid.1");

    assertThat(mensagem.getSentido()).isEqualTo(SentidoMensagem.ENTRADA);
    assertThat(mensagem.getWhatsappMessageId()).isEqualTo("wamid.1");
    assertThat(mensagem.isEnvioPendente()).isFalse();
  }

  @Test
  void deveConfirmarEnvio() {
    Mensagem mensagem = Mensagem.saida(CONVERSA, TipoMensagem.TEXTO, "oi");

    mensagem.confirmarEnvio("wamid.out");

    assertThat(mensagem.getWhatsappMessageId()).isEqualTo("wamid.out");
    assertThat(mensagem.getErroEnvio()).isNull();
    assertThat(mensagem.isEnvioPendente()).isFalse();
  }

  @Test
  void deveRegistrarFalhaEnvio() {
    Mensagem mensagem = Mensagem.saida(CONVERSA, TipoMensagem.TEXTO, "oi");

    mensagem.registrarFalhaEnvio("timeout");

    assertThat(mensagem.getErroEnvio()).isEqualTo("timeout");
    assertThat(mensagem.isEnvioPendente()).isTrue();
  }
}
