package com.felipeduan.atendimento.modules.auth.exceptions;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class LoginCredenciaisInvalidasException extends DomainException {

  public LoginCredenciaisInvalidasException() {
    super("E-mail ou senha inválidos.");
  }
}
