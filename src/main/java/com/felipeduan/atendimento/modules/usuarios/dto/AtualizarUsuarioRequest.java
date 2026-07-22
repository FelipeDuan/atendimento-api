package com.felipeduan.atendimento.modules.usuarios.dto;

import com.felipeduan.atendimento.modules.usuarios.enums.PerfilUsuario;
import com.felipeduan.atendimento.modules.usuarios.enums.StatusVinculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(
    @NotBlank(message = "O nome é obrigatório") @Size(max = 255) String nome,
    @NotNull(message = "O perfil é obrigatório") PerfilUsuario perfil,
    @NotNull(message = "O status é obrigatório") StatusVinculo status) {}
