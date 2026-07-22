package com.felipeduan.atendimento.modules.mensagens;

import com.felipeduan.atendimento.modules.conversas.ConversaService;
import com.felipeduan.atendimento.modules.conversas.dto.ConversaParaMensagem;
import com.felipeduan.atendimento.modules.mensagens.dto.EnviarMensagemRequest;
import com.felipeduan.atendimento.modules.mensagens.dto.MensagemResponse;
import com.felipeduan.atendimento.modules.mensagens.enums.TipoMensagem;
import com.felipeduan.atendimento.modules.mensagens.exception.MensagemNaoEncontradaException;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MensagemService {

  private final ConversaService conversaService;
  private final MensagemRepository mensagemRepository;
  private final MensagemMapper mensagemMapper;

  @Transactional(readOnly = true)
  public PageResponse<MensagemResponse> listar(UUID conversaId, Pageable pageable) {
    conversaService.buscar(conversaId);
    return PageResponse.of(
        mensagemRepository.findByConversaId(conversaId, pageable).map(mensagemMapper::toResponse));
  }

  @Transactional(readOnly = true)
  public MensagemResponse buscar(UUID id) {
    return mensagemMapper.toResponse(carregar(id));
  }

  @Transactional
  public MensagemResponse enviar(EnviarMensagemRequest request) {
    ConversaParaMensagem conversa =
        conversaService.prepararRegistroDeMensagem(request.conversaId());
    Mensagem mensagem = Mensagem.saida(conversa, request.tipo(), request.conteudo());
    return mensagemMapper.toResponse(mensagemRepository.save(mensagem));
  }

  @Transactional
  public void registrarRecebida(
      UUID contatoId, TipoMensagem tipo, String conteudo, String whatsappMessageId) {

    if (mensagemRepository.existsByWhatsappMessageId(whatsappMessageId)) {
      return;
    }

    UUID conversaId = conversaService.garantirConversaAberta(contatoId);
    ConversaParaMensagem conversa = conversaService.prepararRegistroDeMensagem(conversaId);
    Mensagem mensagem = Mensagem.entrada(conversa, tipo, conteudo, whatsappMessageId);
    mensagemRepository.save(mensagem);
  }

  @Transactional
  public MensagemResponse confirmarEnvio(UUID mensagemId, String whatsappMessageId) {
    Mensagem mensagem = carregar(mensagemId);
    mensagem.confirmarEnvio(whatsappMessageId);
    return mensagemMapper.toResponse(mensagemRepository.save(mensagem));
  }

  @Transactional
  public MensagemResponse registrarFalhaEnvio(UUID mensagemId, String motivo) {
    Mensagem mensagem = carregar(mensagemId);
    mensagem.registrarFalhaEnvio(motivo);
    return mensagemMapper.toResponse(mensagemRepository.save(mensagem));
  }

  private Mensagem carregar(UUID id) {
    return mensagemRepository
        .findById(id)
        .orElseThrow(() -> new MensagemNaoEncontradaException(id));
  }
}
