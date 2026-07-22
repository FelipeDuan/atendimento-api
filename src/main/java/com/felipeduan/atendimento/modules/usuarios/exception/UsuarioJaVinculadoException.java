package com.felipeduan.atendimento.modules.usuarios.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class UsuarioJaVinculadoException extends DomainException {

  public UsuarioJaVinculadoException() {
    super("Este usuário já está vinculado a esta empresa.");
  }
}
