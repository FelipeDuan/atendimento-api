package com.felipeduan.atendimento.modules.platformadmin;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorPlataformaRepository extends JpaRepository<AdministradorPlataforma, UUID> {

    Optional<AdministradorPlataforma> findByEmail(String email);
}
