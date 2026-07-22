package com.felipeduan.atendimento.modules.usuarios.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class UltimoAdministradorException extends DomainException {

  public UltimoAdministradorException() {
    super("Não é permitido desativar ou rebaixar o último administrador ativo da empresa.");
  }
}
