package com.felipeduan.atendimento.modules.conversas.dto;

import com.felipeduan.atendimento.modules.conversas.enums.SentidoMensagem;
import com.felipeduan.atendimento.modules.conversas.enums.TipoMensagem;
import java.time.Instant;
import java.util.UUID;

public record MensagemResponse(
    UUID id,
    UUID conversaId,
    TipoMensagem tipo,
    String conteudo,
    SentidoMensagem sentido,
    String whatsappMessageId,
    String erroEnvio,
    Instant dataHora,
    boolean envioPendente) {}
