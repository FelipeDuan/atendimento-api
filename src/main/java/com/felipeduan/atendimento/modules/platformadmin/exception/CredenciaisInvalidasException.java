package com.felipeduan.atendimento.modules.platformadmin.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class CredenciaisInvalidasException extends DomainException {

  public CredenciaisInvalidasException() {
    super("E-mail ou senha inválidos.");
  }
}
