package com.felipeduan.atendimento.support;

import com.felipeduan.atendimento.modules.empresas.Empresa;
import com.felipeduan.atendimento.modules.empresas.EmpresaRepository;
import java.util.UUID;

public final class DadosTesteEmpresa {

  private DadosTesteEmpresa() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static UUID criar(EmpresaRepository repository, String nome) {
    String cnpj = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
    String email = cnpj + "@empresa.test";
    Empresa empresa = new Empresa(nome, cnpj, email);

    return repository.save(empresa).getId();
  }
}
