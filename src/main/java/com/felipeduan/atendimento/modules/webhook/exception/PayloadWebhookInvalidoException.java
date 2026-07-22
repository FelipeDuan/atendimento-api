package com.felipeduan.atendimento.modules.webhook.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class PayloadWebhookInvalidoException extends DomainException {

  public PayloadWebhookInvalidoException() {
    super("Payload do webhook inválido.");
  }
}
