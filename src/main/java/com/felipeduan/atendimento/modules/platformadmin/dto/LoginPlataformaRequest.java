package com.felipeduan.atendimento.modules.platformadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginPlataformaRequest(
	@NotBlank @Email String email,
	@NotBlank String senha) {
}
