package com.felipeduan.atendimento.modules.empresas.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;
import java.util.UUID;

public class EmpresaNaoEncontradaException extends DomainException {

  public EmpresaNaoEncontradaException(UUID id) {
    super("Empresa não encontrada: " + id);
  }
}
