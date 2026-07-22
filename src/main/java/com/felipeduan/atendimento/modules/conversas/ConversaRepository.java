package com.felipeduan.atendimento.modules.conversas;

import com.felipeduan.atendimento.modules.conversas.enums.StatusConversa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversaRepository extends JpaRepository<Conversa, UUID> {

  Page<Conversa> findByStatus(StatusConversa status, Pageable pageable);

  Optional<Conversa> findFirstByContatoIdOrderByDataCriacaoDesc(UUID contatoId);
}
