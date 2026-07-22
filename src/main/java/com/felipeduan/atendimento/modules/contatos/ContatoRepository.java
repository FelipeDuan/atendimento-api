package com.felipeduan.atendimento.modules.contatos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContatoRepository extends JpaRepository<Contato, UUID> {

  List<Contato> findAllByOrderByNomeAsc();

  Optional<Contato> findByNumeroWhatsapp(String numeroWhatsapp);
}
