package com.felipeduan.atendimento.modules.empresas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AtualizarEmpresaRequest(
    @NotBlank(message = "O nome é obrigatório") String nome,
    @NotBlank(message = "O e-mail é obrigatório") @Email(message = "Informe um e-mail válido")
        String email,
    String phoneNumberId) {}
