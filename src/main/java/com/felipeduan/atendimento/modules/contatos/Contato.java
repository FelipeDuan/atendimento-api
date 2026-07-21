package com.felipeduan.atendimento.modules.contatos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

  @Column(nullable = false)
  private String status;

  @Column(name = "data_criacao", nullable = false)
  private Instant dataCriacao;

  public Contato(UUID empresaId, String nome, String numeroWhatsapp) {
    this.empresaId = empresaId;
    this.nome = nome;
    this.numeroWhatsapp = numeroWhatsapp;
    this.status = "ATIVO";
    this.dataCriacao = Instant.now();
  }
}
