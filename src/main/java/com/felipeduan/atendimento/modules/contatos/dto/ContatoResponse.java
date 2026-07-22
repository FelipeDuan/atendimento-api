package com.felipeduan.atendimento.modules.contatos.dto;

import com.felipeduan.atendimento.modules.contatos.enums.StatusContato;
import java.time.Instant;
import java.util.UUID;

public record ContatoResponse(
    UUID id,
    String nome,
    String numeroWhatsapp,
    String email,
    String observacoes,
    StatusContato status,
    Instant dataCriacao) {}
