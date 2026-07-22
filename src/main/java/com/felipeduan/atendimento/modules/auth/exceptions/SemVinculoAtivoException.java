package com.felipeduan.atendimento.modules.auth.exceptions;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class SemVinculoAtivoException extends DomainException {

  public SemVinculoAtivoException() {
    super("Usuário não possui vínculo ativo com nenhuma empresa.");
  }
}
