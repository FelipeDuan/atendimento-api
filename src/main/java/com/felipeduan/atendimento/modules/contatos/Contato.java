package com.felipeduan.atendimento.modules.contatos;

import com.felipeduan.atendimento.modules.contatos.enums.StatusContato;
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
@Table(name = "contato")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contato {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "empresa_id", nullable = false)
  private UUID empresaId;

  @Column(nullable = false)
  private String nome;

  @Column(name = "numero_whatsapp", nullable = false)
  private String numeroWhatsapp;

  private String email;

  private String observacoes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusContato status;

  @Column(name = "data_criacao", nullable = false)
  private Instant dataCriacao;

  public Contato(
      UUID empresaId, String nome, String numeroWhatsapp, String email, String observacoes) {
    this.empresaId = empresaId;
    this.nome = nome;
    this.numeroWhatsapp = numeroWhatsapp;
    this.email = email;
    this.observacoes = observacoes;
    this.status = StatusContato.ATIVO;
    this.dataCriacao = Instant.now();
  }

  public void atualizar(String nome, String email, String observacoes) {
    this.nome = nome;
    this.email = email;
    this.observacoes = observacoes;
  }

  public void inativar() {
    this.status = StatusContato.INATIVO;
  }

  public void reativar() {
    this.status = StatusContato.ATIVO;
  }

  public boolean estaInativo() {
    return status == StatusContato.INATIVO;
  }
}
