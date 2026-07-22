package com.felipeduan.atendimento.modules.conversas;

import com.felipeduan.atendimento.modules.conversas.dto.ConversaResponse;
import com.felipeduan.atendimento.modules.conversas.dto.MensagemResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversaMapper {

  ConversaResponse toResponse(Conversa conversa);

  MensagemResponse toResponse(Mensagem mensagem);
}
