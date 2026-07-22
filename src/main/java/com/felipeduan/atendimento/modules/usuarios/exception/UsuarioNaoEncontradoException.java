package com.felipeduan.atendimento.modules.usuarios.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;
import java.util.UUID;

public class UsuarioNaoEncontradoException extends DomainException {

  public UsuarioNaoEncontradoException(UUID id) {
    super("Usuário não encontrado: " + id);
  }
}
