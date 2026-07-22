package com.felipeduan.atendimento.modules.conversas.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AbrirConversaRequest(@NotNull(message = "O contato é obrigatório") UUID contatoId) {}
