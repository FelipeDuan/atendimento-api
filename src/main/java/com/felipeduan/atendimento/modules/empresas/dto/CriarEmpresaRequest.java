package com.felipeduan.atendimento.modules.empresas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarEmpresaRequest(
    @NotBlank String nome,
    @NotBlank @Size(min = 14, max = 14) String cnpj,
    @NotBlank @Email String email,
    @Valid @NotNull @JsonProperty("administradorInicial") AdminInicialRequest adminInicial) {}
