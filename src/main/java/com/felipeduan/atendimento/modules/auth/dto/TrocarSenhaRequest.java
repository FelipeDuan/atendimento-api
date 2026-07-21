package com.felipeduan.atendimento.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TrocarSenhaRequest(@NotBlank String senhaAtual, @NotBlank String novaSenha) {}
