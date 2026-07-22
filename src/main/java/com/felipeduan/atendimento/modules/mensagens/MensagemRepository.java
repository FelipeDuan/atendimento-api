package com.felipeduan.atendimento.modules.mensagens;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemRepository extends JpaRepository<Mensagem, UUID> {

  Page<Mensagem> findByConversaId(UUID conversaId, Pageable pageable);

  boolean existsByWhatsappMessageId(String whatsappMessageId);

  Optional<Mensagem> findByWhatsappMessageId(String whatsappMessageId);
}
