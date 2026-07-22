package com.felipeduan.atendimento.modules.vinculos;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario_empresa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsuarioEmpresa {

  @EmbeddedId private UsuarioEmpresaId id;

  @Column(nullable = false)
  private String perfil;

  @Column(nullable = false)
  private String status;

  @Column(name = "data_vinculo", nullable = false)
  private Instant dataVinculo;

  public UsuarioEmpresa(UUID usuarioId, UUID empresaId, String perfil) {
    this.id = new UsuarioEmpresaId(usuarioId, empresaId);
    this.perfil = perfil;
    this.status = "ATIVO";
    this.dataVinculo = Instant.now();
  }

  public void atualizar(String perfil, String status) {
    this.perfil = perfil;
    this.status = status;
  }

  public boolean ehAdministradorAtivo() {
    return "ADMINISTRADOR".equals(perfil) && "ATIVO".equals(status);
  }
}
