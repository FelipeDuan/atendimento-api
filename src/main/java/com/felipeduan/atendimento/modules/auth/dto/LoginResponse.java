package com.felipeduan.atendimento.modules.auth.dto;

import java.util.List;

public record LoginResponse(
    String accessToken,
    boolean exigeTrocarSenha,
    List<EmpresaVinculadaResponse> empresasVinculadas) {}
