package com.felipeduan.atendimento.modules.conversas;

import com.felipeduan.atendimento.modules.conversas.enums.SentidoMensagem;
import com.felipeduan.atendimento.modules.conversas.enums.TipoMensagem;
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

  Mensagem(
      Conversa conversa,
      TipoMensagem tipo,
      String conteudo,
      SentidoMensagem sentido,
      String whatsappMessageId) {

    this.empresaId = conversa.getEmpresaId();
    this.conversaId = conversa.getId();
    this.tipo = tipo;
    this.conteudo = conteudo;
    this.sentido = sentido;
    this.whatsappMessageId = whatsappMessageId;
    this.dataHora = Instant.now();
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
