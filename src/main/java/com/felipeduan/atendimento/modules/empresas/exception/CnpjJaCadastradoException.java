package com.felipeduan.atendimento.modules.empresas.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class CnpjJaCadastradoException extends DomainException {

  public CnpjJaCadastradoException() {
    super("Já existe empresa cadastrada com este CNPJ.");
  }
}
