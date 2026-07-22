package com.felipeduan.atendimento.modules.contatos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarContatoRequest(
    @NotBlank(message = "O nome é obrigatório") @Size(max = 255) String nome,
    @Size(max = 255) String email,
    String observacoes) {}
