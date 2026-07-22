package com.felipeduan.atendimento.modules.mensagens.dto;

import com.felipeduan.atendimento.modules.mensagens.enums.TipoMensagem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EnviarMensagemRequest(
    @NotNull(message = "A conversa é obrigatória") UUID conversaId,
    @NotNull(message = "O tipo é obrigatório") TipoMensagem tipo,
    @NotBlank(message = "O conteúdo é obrigatório") String conteudo) {}
