package com.felipeduan.atendimento.modules.empresas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminInicialRequest(
    @NotBlank String nome,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String senhaTemporaria) {}
