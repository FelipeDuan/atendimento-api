package com.felipeduan.atendimento.modules.conversas;

import com.felipeduan.atendimento.modules.conversas.dto.ConversaParaMensagem;
import com.felipeduan.atendimento.modules.conversas.dto.ConversaResponse;
import com.felipeduan.atendimento.modules.conversas.enums.StatusConversa;
import com.felipeduan.atendimento.modules.conversas.exception.ConversaNaoEncontradaException;
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

  @Transactional
  public UUID garantirConversaAberta(UUID contatoId) {
    return conversaAbertaDoContato(contatoId).getId();
  }

  @Transactional
  public ConversaParaMensagem prepararRegistroDeMensagem(UUID conversaId) {
    Conversa conversa = buscarPorId(conversaId);
    conversa.registrarInteracao();
    conversaRepository.save(conversa);
    return new ConversaParaMensagem(conversa.getId(), conversa.getEmpresaId());
  }

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

  private UUID tenantAtual() {
    return TenantContext.getTenantId().orElseThrow(IllegalStateException::new);
  }
}
