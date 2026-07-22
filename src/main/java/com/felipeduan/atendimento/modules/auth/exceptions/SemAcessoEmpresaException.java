package com.felipeduan.atendimento.modules.auth.exceptions;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class SemAcessoEmpresaException extends DomainException {

  public SemAcessoEmpresaException() {
    super("Sem acesso a esta empresa.");
  }
}
