package com.felipeduan.atendimento.shared.web;

import com.felipeduan.atendimento.shared.exception.DomainException;
import java.util.Collection;

public class OrdenacaoInvalidaException extends DomainException {

  public OrdenacaoInvalidaException(String campo, Collection<String> permitidos) {
    super(
        "Não é possível ordenar por '"
            + campo
            + "'. Campos aceitos: "
            + String.join(", ", permitidos));
  }
}
