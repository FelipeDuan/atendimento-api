package com.felipeduan.atendimento.modules.conversas.dto;

import com.felipeduan.atendimento.modules.conversas.enums.AcaoConversa;
import jakarta.validation.constraints.NotNull;

public record AtualizarConversaRequest(
    @NotNull(message = "A ação é obrigatória") AcaoConversa acao) {}
