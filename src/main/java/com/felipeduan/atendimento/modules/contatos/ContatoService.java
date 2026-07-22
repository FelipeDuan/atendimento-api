package com.felipeduan.atendimento.modules.contatos;

import com.felipeduan.atendimento.modules.contatos.dto.AtualizarContatoRequest;
import com.felipeduan.atendimento.modules.contatos.dto.ContatoResponse;
import com.felipeduan.atendimento.modules.contatos.dto.CriarContatoRequest;
import com.felipeduan.atendimento.modules.contatos.enums.StatusContato;
import com.felipeduan.atendimento.modules.contatos.exception.ContatoNaoEncontradoException;
import com.felipeduan.atendimento.modules.contatos.exception.NumeroWhatsappJaCadastradoException;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContatoService {

  private final ContatoRepository repository;
  private final ContatoMapper mapper;

  @Transactional
  public ContatoResponse criar(CriarContatoRequest request) {
    return repository
        .findByNumeroWhatsapp(request.numeroWhatsapp())
        .map(existente -> reaproveitarOuConflitar(existente, request))
        .orElseGet(() -> criarNovo(request));
  }

  @Transactional(readOnly = true)
  public PageResponse<ContatoResponse> listar(Pageable pageable) {
    return PageResponse.of(
        repository.findByStatus(StatusContato.ATIVO, pageable).map(mapper::toResponse));
  }

  @Transactional(readOnly = true)
  public ContatoResponse buscar(UUID id) {
    return mapper.toResponse(carregarAtivo(id));
  }

  @Transactional
  public ContatoResponse atualizar(UUID id, AtualizarContatoRequest request) {
    Contato contato = carregarAtivo(id);
    contato.atualizar(request.nome(), request.email(), request.observacoes());
    return mapper.toResponse(repository.save(contato));
  }

  @Transactional
  public void inativar(UUID id) {
    Contato contato = carregarAtivo(id);
    contato.inativar();
    repository.save(contato);
  }

  @Transactional
  public UUID localizarOuCriar(String numeroWhatsapp, String nome) {
    return repository
        .findByNumeroWhatsapp(numeroWhatsapp)
        .map(this::reativarSeNecessario)
        .orElseGet(() -> criarPorNumero(numeroWhatsapp, nome));
  }

  @Transactional(readOnly = true)
  public void exigirExistente(UUID contatoId) {
    carregarAtivo(contatoId);
  }

  @Transactional(readOnly = true)
  public String buscarNumeroWhatsapp(UUID contatoId) {
    return carregarAtivo(contatoId).getNumeroWhatsapp();
  }

  private ContatoResponse reaproveitarOuConflitar(Contato existente, CriarContatoRequest request) {
    if (!existente.estaInativo()) {
      throw new NumeroWhatsappJaCadastradoException();
    }
    existente.reativar();
    existente.atualizar(request.nome(), request.email(), request.observacoes());
    return mapper.toResponse(repository.save(existente));
  }

  private ContatoResponse criarNovo(CriarContatoRequest request) {
    Contato contato =
        new Contato(
            tenantAtual(),
            request.nome(),
            request.numeroWhatsapp(),
            request.email(),
            request.observacoes());
    return mapper.toResponse(repository.save(contato));
  }

  private UUID reativarSeNecessario(Contato contato) {
    if (contato.estaInativo()) {
      contato.reativar();
      repository.save(contato);
    }
    return contato.getId();
  }

  private UUID criarPorNumero(String numeroWhatsapp, String nome) {
    Contato contato = new Contato(tenantAtual(), nome, numeroWhatsapp, null, null);
    return repository.save(contato).getId();
  }

  private Contato carregarAtivo(UUID id) {
    return repository
        .findByIdAndStatus(id, StatusContato.ATIVO)
        .orElseThrow(() -> new ContatoNaoEncontradoException(id));
  }

  private UUID tenantAtual() {
    return TenantContext.getTenantId().orElseThrow(IllegalStateException::new);
  }
}
