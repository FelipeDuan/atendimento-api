package com.felipeduan.atendimento.modules.conversas.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class EstadoConversaInvalidoException extends DomainException {

  public EstadoConversaInvalidoException(String mensagem) {
    super(mensagem);
  }
}
