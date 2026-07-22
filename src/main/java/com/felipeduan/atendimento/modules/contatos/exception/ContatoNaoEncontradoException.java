package com.felipeduan.atendimento.modules.contatos.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;
import java.util.UUID;

public class ContatoNaoEncontradoException extends DomainException {

  public ContatoNaoEncontradoException(UUID id) {
    super("Contato não encontrado: " + id);
  }
}
