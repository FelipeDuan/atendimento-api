package com.felipeduan.atendimento.modules.mensagens;

import com.felipeduan.atendimento.modules.mensagens.enums.SentidoMensagem;
import com.felipeduan.atendimento.modules.mensagens.enums.TipoMensagem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mensagem")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mensagem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "empresa_id", nullable = false)
  private UUID empresaId;

  @Column(name = "conversa_id", nullable = false)
  private UUID conversaId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoMensagem tipo;

  @Column(nullable = false)
  private String conteudo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SentidoMensagem sentido;

  @Column(name = "whatsapp_message_id")
  private String whatsappMessageId;

  @Column(name = "erro_envio")
  private String erroEnvio;

  @Column(name = "data_hora", nullable = false)
  private Instant dataHora;

  private Mensagem(
      UUID empresaId,
      UUID conversaId,
      TipoMensagem tipo,
      String conteudo,
      SentidoMensagem sentido,
      String whatsappMessageId) {

    this.empresaId = empresaId;
    this.conversaId = conversaId;
    this.tipo = tipo;
    this.conteudo = conteudo;
    this.sentido = sentido;
    this.whatsappMessageId = whatsappMessageId;
    this.dataHora = Instant.now();
  }

  public static Mensagem saida(
      UUID empresaId, UUID conversaId, TipoMensagem tipo, String conteudo) {
    return new Mensagem(empresaId, conversaId, tipo, conteudo, SentidoMensagem.SAIDA, null);
  }

  public static Mensagem entrada(
      UUID empresaId,
      UUID conversaId,
      TipoMensagem tipo,
      String conteudo,
      String whatsappMessageId) {
    return new Mensagem(
        empresaId, conversaId, tipo, conteudo, SentidoMensagem.ENTRADA, whatsappMessageId);
  }

  public void confirmarEnvio(String whatsappMessageId) {
    this.whatsappMessageId = whatsappMessageId;
    this.erroEnvio = null;
  }

  public void registrarFalhaEnvio(String motivo) {
    this.erroEnvio = motivo;
  }

  public boolean isEnvioPendente() {
    return sentido == SentidoMensagem.SAIDA && whatsappMessageId == null;
  }
}
