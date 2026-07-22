package com.felipeduan.atendimento.modules.mensagens.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;
import java.util.UUID;

public class MensagemNaoEncontradaException extends DomainException {

  public MensagemNaoEncontradaException(UUID id) {
    super("Mensagem não encontrada: " + id);
  }
}
