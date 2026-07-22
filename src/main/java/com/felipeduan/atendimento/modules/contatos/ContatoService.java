package com.felipeduan.atendimento.modules.contatos;

import com.felipeduan.atendimento.modules.contatos.exception.ContatoNaoEncontradoException;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContatoService {

  private final ContatoRepository repository;

  public ContatoService(ContatoRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public Contato salvar(Contato contato) {
    return repository.save(contato);
  }

  @Transactional(readOnly = true)
  public List<Contato> listarTodos() {
    return repository.findAllByOrderByNomeAsc();
  }

  @Transactional
  public UUID localizarOuCriar(String numeroWhatsapp, String nome) {
    return repository
        .findByNumeroWhatsapp(numeroWhatsapp)
        .map(Contato::getId)
        .orElseGet(() -> criar(numeroWhatsapp, nome));
  }

  @Transactional(readOnly = true)
  public String buscarNumeroWhatsapp(UUID contatoId) {
    return repository
        .findById(contatoId)
        .map(Contato::getNumeroWhatsapp)
        .orElseThrow(() -> new ContatoNaoEncontradoException(contatoId));
  }

  private UUID criar(String numeroWhatsapp, String nome) {
    UUID empresaId = TenantContext.getTenantId().orElseThrow();
    return repository.save(new Contato(empresaId, nome, numeroWhatsapp)).getId();
  }
}
