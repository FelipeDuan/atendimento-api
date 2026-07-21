package com.felipeduan.atendimento.modules.empresas;

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
@Table(name = "empresa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Empresa {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false, unique = true, length = 14)
  private String cnpj;

  @Column(nullable = false)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EmpresaStatus status;

  @Column(name = "phone_number_id")
  private String phoneNumberId;

  @Column(name = "data_criacao", nullable = false)
  private Instant dataCriacao;

  public Empresa(String nome, String cnpj, String email) {
    this.nome = nome;
    this.cnpj = cnpj;
    this.email = email;
    this.status = EmpresaStatus.ATIVA;
    this.dataCriacao = Instant.now();
  }
}
