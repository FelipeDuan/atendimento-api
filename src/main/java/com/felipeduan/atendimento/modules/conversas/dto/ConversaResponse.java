package com.felipeduan.atendimento.modules.conversas.dto;

import com.felipeduan.atendimento.modules.conversas.enums.StatusConversa;
import java.time.Instant;
import java.util.UUID;

public record ConversaResponse(
    UUID id,
    UUID contatoId,
    StatusConversa status,
    UUID responsavelId,
    UUID conversaAnteriorId,
    Instant ultimaInteracao,
    Instant dataCriacao,
    Instant dataEncerramento) {}
