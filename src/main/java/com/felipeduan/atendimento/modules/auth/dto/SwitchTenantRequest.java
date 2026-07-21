package com.felipeduan.atendimento.modules.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SwitchTenantRequest(
    @NotNull(message = "O identificador da empresa é obrigatório") UUID empresaId) {}
