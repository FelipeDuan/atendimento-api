package com.felipeduan.atendimento.modules.webhook;

import com.felipeduan.atendimento.modules.contatos.ContatoService;
import com.felipeduan.atendimento.modules.empresas.EmpresaService;
import com.felipeduan.atendimento.modules.mensagens.MensagemService;
import com.felipeduan.atendimento.modules.webhook.dto.MetaWebhookPayload;
import com.felipeduan.atendimento.modules.webhook.exception.PayloadWebhookInvalidoException;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class WebhookService {

  private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

  private final JsonMapper jsonMapper;
  private final EmpresaService empresaService;
  private final ContatoService contatoService;
  private final MensagemService mensagemService;

  public void processar(byte[] corpoBruto) {
    MetaWebhookPayload payload = desserializar(corpoBruto);
    for (MensagemRecebida recebida : MensagemRecebida.extrairDe(payload)) {
      processarMensagem(recebida);
    }
  }

  private void processarMensagem(MensagemRecebida recebida) {
    Optional<UUID> empresaId = empresaService.buscarIdPorPhoneNumberId(recebida.phoneNumberId());

    if (empresaId.isEmpty()) {
      log.warn("Webhook recebido para phone_number_id desconhecido");
      return;
    }

    TenantContext.withTenantId(empresaId.get(), () -> registrar(recebida));
  }

  private void registrar(MensagemRecebida recebida) {
    UUID contatoId = contatoService.localizarOuCriar(recebida.numero(), recebida.nome());

    try {
      mensagemService.registrarRecebida(
          contatoId, recebida.tipo(), recebida.conteudo(), recebida.whatsappMessageId());
    } catch (DataIntegrityViolationException e) {
      log.info("Mensagem já processada (reentrega da Meta): {}", recebida.whatsappMessageId());
    }
  }

  private MetaWebhookPayload desserializar(byte[] corpoBruto) {
    try {
      return jsonMapper.readValue(corpoBruto, MetaWebhookPayload.class);
    } catch (JacksonException e) {
      throw new PayloadWebhookInvalidoException();
    }
  }
}
