package com.felipeduan.atendimento.modules.conversas;

import com.felipeduan.atendimento.modules.conversas.dto.ConversaResponse;
import com.felipeduan.atendimento.modules.conversas.dto.MensagemResponse;
import com.felipeduan.atendimento.modules.conversas.enums.SentidoMensagem;
import com.felipeduan.atendimento.modules.conversas.enums.StatusConversa;
import com.felipeduan.atendimento.modules.conversas.enums.TipoMensagem;
import com.felipeduan.atendimento.modules.conversas.exception.ConversaNaoEncontradaException;
import com.felipeduan.atendimento.modules.conversas.exception.MensagemNaoEncontradaException;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversaService {

  private final ConversaRepository conversaRepository;
  private final MensagemRepository mensagemRepository;
  private final ConversaMapper conversaMapper;

  @Transactional(readOnly = true)
  public PageResponse<ConversaResponse> listar(StatusConversa status, Pageable pageable) {
    Page<Conversa> pagina =
        status == null
            ? conversaRepository.findAll(pageable)
            : conversaRepository.findByStatus(status, pageable);

    return PageResponse.of(pagina.map(conversaMapper::toResponse));
  }

  @Transactional(readOnly = true)
  public ConversaResponse buscar(UUID id) {
    return conversaMapper.toResponse(buscarPorId(id));
  }

  @Transactional
  public ConversaResponse encerrar(UUID id) {
    Conversa conversa = buscarPorId(id);
    conversa.encerrar();
    return conversaMapper.toResponse(conversaRepository.save(conversa));
  }

  @Transactional
  public ConversaResponse reabrir(UUID id) {
    Conversa conversa = buscarPorId(id);
    conversa.reabrir();
    return conversaMapper.toResponse(conversaRepository.save(conversa));
  }

  @Transactional(readOnly = true)
  public PageResponse<MensagemResponse> listarMensagens(UUID conversaId, Pageable pageable) {
    buscarPorId(conversaId);
    return PageResponse.of(
        mensagemRepository.findByConversaId(conversaId, pageable).map(conversaMapper::toResponse));
  }

  @Transactional
  public MensagemResponse enviarMensagemDeSaida(
      UUID conversaId, TipoMensagem tipo, String conteudo) {

    Conversa conversa = buscarPorId(conversaId);
    Mensagem mensagem = conversa.registrarMensagem(tipo, conteudo, SentidoMensagem.SAIDA, null);

    conversaRepository.save(conversa);
    return conversaMapper.toResponse(mensagemRepository.save(mensagem));
  }

  @Transactional
  public UUID garantirConversaAberta(UUID contatoId) {
    return conversaAbertaDoContato(contatoId).getId();
  }

  @Transactional
  public void registrarMensagemRecebida(
      UUID contatoId, TipoMensagem tipo, String conteudo, String whatsappMessageId) {

    if (mensagemRepository.existsByWhatsappMessageId(whatsappMessageId)) {
      return;
    }

    Conversa conversa = conversaAbertaDoContato(contatoId);
    Mensagem mensagem =
        conversa.registrarMensagem(tipo, conteudo, SentidoMensagem.ENTRADA, whatsappMessageId);

    conversaRepository.save(conversa);
    mensagemRepository.save(mensagem);
  }

  @Transactional
  public MensagemResponse confirmarEnvio(UUID mensagemId, String whatsappMessageId) {
    Mensagem mensagem = buscarMensagem(mensagemId);
    mensagem.confirmarEnvio(whatsappMessageId);
    return conversaMapper.toResponse(mensagemRepository.save(mensagem));
  }

  @Transactional
  public MensagemResponse registrarFalhaEnvio(UUID mensagemId, String motivo) {
    Mensagem mensagem = buscarMensagem(mensagemId);
    mensagem.registrarFalhaEnvio(motivo);
    return conversaMapper.toResponse(mensagemRepository.save(mensagem));
  }

  // abaixo são passos privados

  private Conversa conversaAbertaDoContato(UUID contatoId) {
    Optional<Conversa> ultima =
        conversaRepository.findFirstByContatoIdOrderByDataCriacaoDesc(contatoId);

    if (ultima.isEmpty()) {
      return conversaRepository.save(Conversa.abrir(tenantAtual(), contatoId));
    }

    Conversa conversa = ultima.get();
    if (conversa.estaEncerrada()) {
      return conversaRepository.save(Conversa.continuacaoDe(conversa));
    }

    return conversa;
  }

  private Conversa buscarPorId(UUID id) {
    return conversaRepository
        .findById(id)
        .orElseThrow(() -> new ConversaNaoEncontradaException(id));
  }

  private Mensagem buscarMensagem(UUID id) {
    return mensagemRepository
        .findById(id)
        .orElseThrow(() -> new MensagemNaoEncontradaException(id));
  }

  private UUID tenantAtual() {
    return TenantContext.getTenantId().orElseThrow(IllegalStateException::new);
  }
}
