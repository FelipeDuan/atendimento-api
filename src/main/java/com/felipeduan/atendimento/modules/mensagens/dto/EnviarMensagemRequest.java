package com.felipeduan.atendimento.modules.mensagens.dto;

import com.felipeduan.atendimento.modules.conversas.enums.TipoMensagem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnviarMensagemRequest(
    @NotNull(message = "O tipo é obrigatório") TipoMensagem tipo,
    @NotBlank(message = "O conteúdo é obrigatório") String conteudo) {}
