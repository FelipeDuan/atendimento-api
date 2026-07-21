package com.felipeduan.atendimento.modules.empresas;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

  Page<Empresa> findByStatus(EmpresaStatus status, Pageable pageable);

  @Query(
      value =
          """
          SELECT id, nome, cnpj, email, status, phone_number_id, data_criacao
          FROM empresa
          WHERE status = :status
          ORDER BY data_criacao DESC
          """,
      countQuery = "SELECT count(*) FROM empresa WHERE status = :status",
      nativeQuery = true)
  Page<Object[]> findPaginaResumoPorStatus(@Param("status") String status, Pageable pageable);

  @Query(value = "SELECT EXISTS(SELECT 1 FROM empresa WHERE cnpj = :cnpj)", nativeQuery = true)
  boolean existsByCnpj(@Param("cnpj") String cnpj);
}
