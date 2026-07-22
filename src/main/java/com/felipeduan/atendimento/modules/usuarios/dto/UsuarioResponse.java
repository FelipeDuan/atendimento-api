package com.felipeduan.atendimento.modules.usuarios.dto;

import com.felipeduan.atendimento.modules.usuarios.enums.PerfilUsuario;
import com.felipeduan.atendimento.modules.usuarios.enums.StatusVinculo;
import java.time.Instant;
import java.util.UUID;

public record UsuarioResponse(
    UUID id,
    String nome,
    String email,
    PerfilUsuario perfil,
    StatusVinculo status,
    Instant dataCriacao) {}
