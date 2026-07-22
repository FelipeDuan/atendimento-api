package com.felipeduan.atendimento.modules.conversas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.felipeduan.atendimento.modules.conversas.enums.SentidoMensagem;
import com.felipeduan.atendimento.modules.conversas.enums.StatusConversa;
import com.felipeduan.atendimento.modules.conversas.enums.TipoMensagem;
import com.felipeduan.atendimento.modules.conversas.exception.ConversaEncerradaException;
import com.felipeduan.atendimento.modules.conversas.exception.EstadoConversaInvalidoException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversaTest {

  private static final UUID EMPRESA = UUID.randomUUID();
  private static final UUID CONTATO = UUID.randomUUID();

  @Test
  void naoDeveRegistrarMensagemEmConversaEncerrada() {
    Conversa conversa = Conversa.abrir(EMPRESA, CONTATO);
    conversa.encerrar();

    assertThatThrownBy(
            () ->
                conversa.registrarMensagem(TipoMensagem.TEXTO, "oi", SentidoMensagem.ENTRADA, null))
        .isInstanceOf(ConversaEncerradaException.class);
  }

  @Test
  void deveAtualizarUltimaInteracao_quandoRegistraMensagem() {
    Conversa conversa = Conversa.abrir(EMPRESA, CONTATO);
    Instant antes = conversa.getUltimaInteracao();

    conversa.registrarMensagem(TipoMensagem.TEXTO, "oi", SentidoMensagem.ENTRADA, "wamid.1");

    assertThat(conversa.getUltimaInteracao()).isAfterOrEqualTo(antes);
  }

  @Test
  void naoDeveEncerrarConversaJaEncerrada() {
    Conversa conversa = Conversa.abrir(EMPRESA, CONTATO);
    conversa.encerrar();

    assertThatThrownBy(conversa::encerrar).isInstanceOf(EstadoConversaInvalidoException.class);
  }

  @Test
  void naoDeveReabrirConversaJaAberta() {
    Conversa conversa = Conversa.abrir(EMPRESA, CONTATO);

    assertThatThrownBy(conversa::reabrir).isInstanceOf(EstadoConversaInvalidoException.class);
  }

  @Test
  void deveLimparDataEncerramento_quandoReabre() {
    Conversa conversa = Conversa.abrir(EMPRESA, CONTATO);
    conversa.encerrar();
    assertThat(conversa.getDataEncerramento()).isNotNull();

    conversa.reabrir();

    assertThat(conversa.getStatus()).isEqualTo(StatusConversa.ABERTA);
    assertThat(conversa.getDataEncerramento()).isNull();
  }

  @Test
  void deveApontarParaConversaAnterior_quandoEhContinuacao() {
    Conversa anterior = Conversa.abrir(EMPRESA, CONTATO);

    anterior.encerrar();

    Conversa nova = Conversa.continuacaoDe(anterior);

    assertThat(nova.getConversaAnteriorId()).isEqualTo(anterior.getId());
    assertThat(nova.getEmpresaId()).isEqualTo(EMPRESA);
    assertThat(nova.getContatoId()).isEqualTo(CONTATO);
    assertThat(nova.getStatus()).isEqualTo(StatusConversa.ABERTA);
  }
}
