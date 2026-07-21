package com.felipeduan.atendimento.modules.empresas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "empresa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmpresaRegistro {

  @Id private UUID id;

  private String nome;

  private String cnpj;

  private String email;

  @Enumerated(EnumType.STRING)
  private EmpresaStatus status;

  @Column(name = "phone_number_id")
  private String phoneNumberId;

  @Column(name = "data_criacao")
  private Instant dataCriacao;
}
