package com.felipeduan.atendimento.modules.contatos;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

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

}
