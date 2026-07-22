package com.felipeduan.atendimento.modules.mensagens;

import com.felipeduan.atendimento.modules.mensagens.dto.MensagemResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MensagemMapper {

  MensagemResponse toResponse(Mensagem mensagem);
}
