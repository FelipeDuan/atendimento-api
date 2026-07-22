package com.felipeduan.atendimento.modules.conversas;

import com.felipeduan.atendimento.modules.conversas.enums.StatusConversa;
import com.felipeduan.atendimento.modules.conversas.exception.ConversaEncerradaException;
import com.felipeduan.atendimento.modules.conversas.exception.EstadoConversaInvalidoException;
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
@Table(name = "conversa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversa {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "empresa_id", nullable = false)
  private UUID empresaId;

  @Column(name = "contato_id", nullable = false)
  private UUID contatoId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusConversa status;

  @Column(name = "responsavel_id")
  private UUID responsavelId;

  @Column(name = "previous_conversation_id")
  private UUID conversaAnteriorId;

  @Column(name = "ultima_interacao", nullable = false)
  private Instant ultimaInteracao;

  @Column(name = "data_criacao", nullable = false)
  private Instant dataCriacao;

  @Column(name = "data_encerramento")
  private Instant dataEncerramento;

  private Conversa(UUID empresaId, UUID contatoId, UUID conversaAnteriorId) {
    this.empresaId = empresaId;
    this.contatoId = contatoId;
    this.conversaAnteriorId = conversaAnteriorId;
    this.status = StatusConversa.ABERTA;
    this.dataCriacao = Instant.now();
    this.ultimaInteracao = this.dataCriacao;
  }

  public static Conversa abrir(UUID empresaId, UUID contatoId) {
    return new Conversa(empresaId, contatoId, null);
  }

  public static Conversa continuacaoDe(Conversa anterior) {
    return new Conversa(anterior.empresaId, anterior.contatoId, anterior.id);
  }

  public void exigirAberta() {
    if (estaEncerrada()) {
      throw new ConversaEncerradaException(id);
    }
  }

  public void registrarInteracao() {
    exigirAberta();
    this.ultimaInteracao = Instant.now();
  }

  public void encerrar() {
    if (estaEncerrada()) {
      throw new EstadoConversaInvalidoException("A conversa já está encerrada.");
    }

    this.status = StatusConversa.ENCERRADA;
    this.dataEncerramento = Instant.now();
  }

  public void reabrir() {
    if (!estaEncerrada()) {
      throw new EstadoConversaInvalidoException("A conversa já está aberta.");
    }

    this.status = StatusConversa.ABERTA;
    this.dataEncerramento = null;
    this.ultimaInteracao = Instant.now();
  }

  public void atribuirResponsavel(UUID responsavelId) {
    this.responsavelId = responsavelId;
  }

  public boolean estaEncerrada() {
    return status == StatusConversa.ENCERRADA;
  }
}
