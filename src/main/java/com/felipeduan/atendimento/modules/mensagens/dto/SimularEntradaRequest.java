package com.felipeduan.atendimento.modules.mensagens.dto;

import com.felipeduan.atendimento.modules.mensagens.enums.TipoMensagem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SimularEntradaRequest(
    @NotBlank(message = "O número do WhatsApp é obrigatório") @Size(max = 20) String numeroWhatsapp,
    @Size(max = 255) String nome,
    @NotNull(message = "O tipo é obrigatório") TipoMensagem tipo,
    @NotBlank(message = "O conteúdo é obrigatório") String conteudo,
    @Size(max = 128) String whatsappMessageId) {}
