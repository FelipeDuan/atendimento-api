package com.felipeduan.atendimento.modules.webhook.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class AssinaturaWebhookInvalidaException extends DomainException {

  public AssinaturaWebhookInvalidaException() {
    super("Assinatura do webhook inválida.");
  }
}
