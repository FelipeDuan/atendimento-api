package com.felipeduan.atendimento.modules.empresas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarEmpresaRequest(
    @NotBlank(message = "O nome é obrigatório") String nome,
    @NotBlank(message = "O CNPJ é obrigatório")
        @Size(min = 14, max = 14, message = "O CNPJ deve ter 14 dígitos")
        String cnpj,
    @NotBlank(message = "O e-mail é obrigatório") @Email(message = "Informe um e-mail válido")
        String email,
    @Valid @NotNull(message = "O administrador inicial é obrigatório")
        AdminInicialRequest administradorInicial) {}
