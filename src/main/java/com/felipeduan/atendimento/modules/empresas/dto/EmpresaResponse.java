package com.felipeduan.atendimento.modules.empresas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.felipeduan.atendimento.modules.empresas.EmpresaStatus;
import java.time.Instant;
import java.util.UUID;

public record EmpresaResponse(
    UUID id,
    String nome,
    String cnpj,
    String email,
    EmpresaStatus status,
    Instant dataCriacao,
    @JsonProperty("administradorInicial") AdminInicialResponse adminInicial) {}
