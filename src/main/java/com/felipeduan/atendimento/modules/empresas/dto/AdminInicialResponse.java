package com.felipeduan.atendimento.modules.empresas.dto;

import java.util.UUID;

public record AdminInicialResponse(UUID usuarioId, String email, boolean deveTrocarSenha) {}
