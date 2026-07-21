package com.felipeduan.atendimento.modules.usuarios.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class EmailExistenteSenhaInvalidaException extends DomainException {

  public EmailExistenteSenhaInvalidaException() {
    super("E-mail já possui conta. Informe a senha correta ou solicite acesso ao administrador.");
  }
}
