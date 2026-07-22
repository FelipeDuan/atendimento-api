package com.felipeduan.atendimento.modules.empresas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminInicialRequest(
    @NotBlank(message = "O nome é obrigatório") String nome,
    @NotBlank(message = "O e-mail é obrigatório") @Email(message = "Informe um e-mail válido")
        String email,
    @NotBlank(message = "A senha temporária é obrigatória")
        @Size(min = 8, message = "A senha temporária deve ter no mínimo 8 caracteres")
        String senhaTemporaria) {}
