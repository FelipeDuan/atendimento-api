package com.felipeduan.atendimento.modules.empresas;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

  @Query("SELECT e FROM Empresa e")
  Page<Empresa> findAtivas(Pageable pageable);

  @Query("SELECT e.id FROM Empresa e")
  List<UUID> findIdsAtivas();
}
