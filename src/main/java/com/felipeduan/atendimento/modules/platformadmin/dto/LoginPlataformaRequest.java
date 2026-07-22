package com.felipeduan.atendimento.modules.platformadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginPlataformaRequest(
    @NotBlank(message = "O e-mail é obrigatório") @Email(message = "Informe um e-mail válido")
        String email,
    @NotBlank(message = "A senha é obrigatória") String senha) {}
