package com.felipeduan.atendimento.modules.conversas.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;
import java.util.UUID;

public class ConversaNaoEncontradaException extends DomainException {

  public ConversaNaoEncontradaException(UUID id) {
    super("Conversa não encontrada: " + id);
  }
}
