package com.felipeduan.atendimento.modules.auth.dto;

import java.util.UUID;

public record EmpresaVinculadaResponse(UUID empresaId, String nome, String perfil) {}
