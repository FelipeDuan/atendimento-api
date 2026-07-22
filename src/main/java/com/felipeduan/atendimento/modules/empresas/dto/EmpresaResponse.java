package com.felipeduan.atendimento.modules.empresas.dto;

import com.felipeduan.atendimento.modules.empresas.enums.EmpresaStatus;
import java.time.Instant;
import java.util.UUID;

public record EmpresaResponse(
    UUID id,
    String nome,
    String cnpj,
    String email,
    EmpresaStatus status,
    String phoneNumberId,
    Instant dataCriacao,
    AdminInicialResponse administradorInicial) {}
