package com.felipeduan.atendimento.modules.contatos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarContatoRequest(
    @NotBlank(message = "O nome é obrigatório") @Size(max = 255) String nome,
    @NotBlank(message = "O número do WhatsApp é obrigatório") @Size(max = 20) String numeroWhatsapp,
    @Size(max = 255) String email,
    String observacoes) {}
