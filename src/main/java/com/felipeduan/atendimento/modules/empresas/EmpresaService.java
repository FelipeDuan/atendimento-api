package com.felipeduan.atendimento.modules.empresas;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {
    
    private final EmpresaRepository repository;

    public EmpresaService(EmpresaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Empresa salvar(Empresa empresa) {
        return repository.save(empresa);
    }
}
