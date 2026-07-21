package com.felipeduan.atendimento.modules.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SwitchTenantRequest(@NotNull UUID empresaId) {}
