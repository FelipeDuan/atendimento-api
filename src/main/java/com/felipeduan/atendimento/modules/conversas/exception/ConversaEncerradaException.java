package com.felipeduan.atendimento.modules.conversas.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;
import java.util.UUID;

public class ConversaEncerradaException extends DomainException {

  public ConversaEncerradaException(UUID conversaId) {
    super("Não é possível registrar mensagem em conversa encerrada: " + conversaId);
  }
}
