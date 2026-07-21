package com.felipeduan.atendimento.modules.empresas;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRegistroRepository extends JpaRepository<EmpresaRegistro, UUID> {

  Page<EmpresaRegistro> findByStatus(EmpresaStatus status, Pageable pageable);

  boolean existsByCnpj(String cnpj);

  List<EmpresaRegistro> findByIdIn(Collection<UUID> ids);
}
