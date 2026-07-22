package com.felipeduan.atendimento.modules.contatos;

import com.felipeduan.atendimento.modules.contatos.enums.StatusContato;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContatoRepository extends JpaRepository<Contato, UUID> {

  Page<Contato> findByStatus(StatusContato status, Pageable pageable);

  Optional<Contato> findByIdAndStatus(UUID id, StatusContato status);

  Optional<Contato> findByNumeroWhatsapp(String numeroWhatsapp);
}
