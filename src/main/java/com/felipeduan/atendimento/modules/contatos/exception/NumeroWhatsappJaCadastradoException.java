package com.felipeduan.atendimento.modules.contatos.exception;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class NumeroWhatsappJaCadastradoException extends DomainException {

  public NumeroWhatsappJaCadastradoException() {
    super("Já existe um contato com este número de WhatsApp nesta empresa.");
  }
}
