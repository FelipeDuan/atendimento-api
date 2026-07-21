package com.felipeduan.atendimento.modules.empresas;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

  boolean existsByCnpj(String cnpj);

  List<Empresa> findByStatus(EmpresaStatus status);
}
