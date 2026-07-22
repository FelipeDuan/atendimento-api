package com.felipeduan.atendimento.modules.conversas;

import com.felipeduan.atendimento.modules.conversas.dto.ConversaResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversaMapper {

  ConversaResponse toResponse(Conversa conversa);
}
