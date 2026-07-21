package com.felipeduan.atendimento.modules.empresas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record EmpresaResponse(
    UUID id,
    String nome,
    String cnpj,
    String email,
    String status,
    Instant dataCriacao,
    @JsonProperty("administradorInicial") AdminInicialResponse adminInicial) {}
