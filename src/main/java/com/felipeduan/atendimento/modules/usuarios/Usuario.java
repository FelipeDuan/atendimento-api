package com.felipeduan.atendimento.modules.usuarios;

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
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "senha_hash", nullable = false)
  private String senhaHash;

  @Column(name = "last_empresa_id")
  private UUID lastEmpresaId;

  @Column(name = "deve_trocar_senha", nullable = false)
  private boolean deveTrocarSenha;

  @Column(name = "data_criacao", nullable = false)
  private Instant dataCriacao;

  public static Usuario criarComSenhaTemporaria(
      String nome, String email, String senhaHash, UUID empresaId) {
    var usuario = new Usuario();
    usuario.nome = nome;
    usuario.email = email;
    usuario.senhaHash = senhaHash;
    usuario.deveTrocarSenha = true;
    usuario.lastEmpresaId = empresaId;
    usuario.dataCriacao = Instant.now();
    return usuario;
  }

  public void registrarNovoVinculo(UUID empresaId) {
    this.lastEmpresaId = empresaId;
  }

  public void atualizarNome(String nome) {
    this.nome = nome;
  }

  public void alterarSenha(String novaSenhaHash) {
    this.senhaHash = novaSenhaHash;
    this.deveTrocarSenha = false;
  }
}
