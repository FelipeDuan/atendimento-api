package com.felipeduan.atendimento.modules.mensagens;

import com.felipeduan.atendimento.modules.contatos.ContatoService;
import com.felipeduan.atendimento.modules.conversas.ConversaService;
import com.felipeduan.atendimento.modules.conversas.dto.ConversaParaMensagem;
import com.felipeduan.atendimento.modules.mensagens.dto.EnviarMensagemRequest;
import com.felipeduan.atendimento.modules.mensagens.dto.MensagemResponse;
import com.felipeduan.atendimento.modules.mensagens.dto.SimularEntradaRequest;
import com.felipeduan.atendimento.modules.mensagens.enums.TipoMensagem;
import com.felipeduan.atendimento.modules.mensagens.exception.MensagemNaoEncontradaException;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MensagemService {

  private final ContatoService contatoService;
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
  public MensagemResponse simularEntrada(SimularEntradaRequest request) {
    String nome =
        StringUtils.hasText(request.nome()) ? request.nome().trim() : request.numeroWhatsapp();
    UUID contatoId = contatoService.localizarOuCriar(request.numeroWhatsapp().trim(), nome);
    String whatsappMessageId =
        StringUtils.hasText(request.whatsappMessageId())
            ? request.whatsappMessageId().trim()
            : "sim-" + UUID.randomUUID();

    return mensagemRepository
        .findByWhatsappMessageId(whatsappMessageId)
        .map(mensagemMapper::toResponse)
        .orElseGet(
            () ->
                persistirEntradaIdempotente(
                    contatoId, request.tipo(), request.conteudo(), whatsappMessageId));
  }

  @Transactional
  public void registrarRecebida(
      UUID contatoId, TipoMensagem tipo, String conteudo, String whatsappMessageId) {

    if (mensagemRepository.existsByWhatsappMessageId(whatsappMessageId)) {
      return;
    }

    persistirEntrada(contatoId, tipo, conteudo, whatsappMessageId);
  }

  private MensagemResponse persistirEntradaIdempotente(
      UUID contatoId, TipoMensagem tipo, String conteudo, String whatsappMessageId) {
    try {
      return persistirEntrada(contatoId, tipo, conteudo, whatsappMessageId);
    } catch (DataIntegrityViolationException e) {
      return mensagemRepository
          .findByWhatsappMessageId(whatsappMessageId)
          .map(mensagemMapper::toResponse)
          .orElseThrow(() -> e);
    }
  }

  private MensagemResponse persistirEntrada(
      UUID contatoId, TipoMensagem tipo, String conteudo, String whatsappMessageId) {
    UUID conversaId = conversaService.garantirConversaAberta(contatoId);
    ConversaParaMensagem conversa = conversaService.prepararRegistroDeMensagem(conversaId);
    Mensagem mensagem = Mensagem.entrada(conversa, tipo, conteudo, whatsappMessageId);
    return mensagemMapper.toResponse(mensagemRepository.save(mensagem));
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
