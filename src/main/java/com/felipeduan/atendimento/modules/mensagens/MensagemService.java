package com.felipeduan.atendimento.modules.mensagens;

import com.felipeduan.atendimento.modules.conversas.ConversaService;
import com.felipeduan.atendimento.modules.conversas.dto.MensagemResponse;
import com.felipeduan.atendimento.modules.mensagens.dto.EnviarMensagemRequest;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MensagemService {

  private final ConversaService conversaService;

  public PageResponse<MensagemResponse> listar(UUID conversaId, Pageable pageable) {
    return conversaService.listarMensagens(conversaId, pageable);
  }

  public MensagemResponse enviar(UUID conversaId, EnviarMensagemRequest request) {
    return conversaService.enviarMensagemDeSaida(conversaId, request.tipo(), request.conteudo());
  }
}
