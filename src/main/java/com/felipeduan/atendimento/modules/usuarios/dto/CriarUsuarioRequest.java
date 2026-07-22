package com.felipeduan.atendimento.modules.usuarios.dto;

import com.felipeduan.atendimento.modules.usuarios.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
    @NotBlank(message = "O nome é obrigatório") @Size(max = 255) String nome,
    @NotBlank(message = "O e-mail é obrigatório") @Email String email,
    @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String senha,
    @NotNull(message = "O perfil é obrigatório") PerfilUsuario perfil) {}
