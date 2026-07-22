package com.felipeduan.atendimento.modules.contatos;

import com.felipeduan.atendimento.modules.contatos.dto.ContatoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContatoMapper {

  ContatoResponse toResponse(Contato contato);
}
