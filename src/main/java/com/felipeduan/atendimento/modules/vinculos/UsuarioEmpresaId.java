package com.felipeduan.atendimento.modules.vinculos;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record UsuarioEmpresaId(
    @Column(name = "usuario_id") UUID usuarioId, @Column(name = "empresa_id") UUID empresaId)
    implements Serializable {}
