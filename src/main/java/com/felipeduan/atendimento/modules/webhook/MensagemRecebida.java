package com.felipeduan.atendimento.modules.webhook;

import com.felipeduan.atendimento.modules.mensagens.enums.TipoMensagem;
import com.felipeduan.atendimento.modules.webhook.dto.MetaWebhookPayload;
import com.felipeduan.atendimento.modules.webhook.dto.MetaWebhookPayload.Change;
import com.felipeduan.atendimento.modules.webhook.dto.MetaWebhookPayload.Contact;
import com.felipeduan.atendimento.modules.webhook.dto.MetaWebhookPayload.Entry;
import com.felipeduan.atendimento.modules.webhook.dto.MetaWebhookPayload.Message;
import com.felipeduan.atendimento.modules.webhook.dto.MetaWebhookPayload.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record MensagemRecebida(
    String phoneNumberId,
    String numero,
    String nome,
    String conteudo,
    String whatsappMessageId,
    TipoMensagem tipo) {

  private static final Logger log = LoggerFactory.getLogger(MensagemRecebida.class);

  static List<MensagemRecebida> extrairDe(MetaWebhookPayload payload) {
    List<MensagemRecebida> recebidas = new ArrayList<>();
    if (payload == null || payload.entry() == null) {
      return recebidas;
    }

    for (Entry entry : payload.entry()) {
      if (entry == null || entry.changes() == null) {
        continue;
      }
      for (Change change : entry.changes()) {
        if (change == null || change.value() == null) {
          continue;
        }
        extrairDoValor(change.value(), recebidas);
      }
    }
    return recebidas;
  }

  private static void extrairDoValor(Value value, List<MensagemRecebida> recebidas) {
    if (value.metadata() == null
        || value.metadata().phone_number_id() == null
        || value.messages() == null) {
      return;
    }

    Map<String, String> nomesPorNumero = indexarNomes(value.contacts());
    String phoneNumberId = value.metadata().phone_number_id();

    for (Message message : value.messages()) {
      if (message == null || message.id() == null || message.from() == null) {
        continue;
      }

      ConteudoInterpretado interpretado = interpretar(message);
      if (interpretado == null) {
        continue;
      }

      String nome = nomesPorNumero.getOrDefault(message.from(), message.from());
      recebidas.add(
          new MensagemRecebida(
              phoneNumberId,
              message.from(),
              nome,
              interpretado.conteudo(),
              message.id(),
              interpretado.tipo()));
    }
  }

  private static ConteudoInterpretado interpretar(Message message) {
    if (message.type() == null) {
      return null;
    }

    return switch (message.type()) {
      case "text" -> interpretarTexto(message);
      case "image" -> interpretarImagem(message);
      case "document" -> interpretarDocumento(message);
      default -> {
        log.info("Tipo de mensagem ignorado no webhook: {}", message.type());
        yield null;
      }
    };
  }

  private static ConteudoInterpretado interpretarTexto(Message message) {
    if (message.text() == null || message.text().body() == null) {
      return null;
    }
    return new ConteudoInterpretado(TipoMensagem.TEXTO, message.text().body());
  }

  private static ConteudoInterpretado interpretarImagem(Message message) {
    String caption =
        message.image() != null && message.image().caption() != null
            ? message.image().caption()
            : "[imagem recebida]";
    return new ConteudoInterpretado(TipoMensagem.IMAGEM, caption);
  }

  private static ConteudoInterpretado interpretarDocumento(Message message) {
    if (message.document() != null && message.document().caption() != null) {
      return new ConteudoInterpretado(TipoMensagem.DOCUMENTO, message.document().caption());
    }
    if (message.document() != null && message.document().filename() != null) {
      return new ConteudoInterpretado(TipoMensagem.DOCUMENTO, message.document().filename());
    }
    return new ConteudoInterpretado(TipoMensagem.DOCUMENTO, "[documento recebido]");
  }

  private static Map<String, String> indexarNomes(List<Contact> contacts) {
    Map<String, String> nomes = new HashMap<>();
    if (contacts == null) {
      return nomes;
    }
    for (Contact contact : contacts) {
      if (contact == null || contact.wa_id() == null) {
        continue;
      }
      String nome =
          contact.profile() != null && contact.profile().name() != null
              ? contact.profile().name()
              : contact.wa_id();
      nomes.put(contact.wa_id(), nome);
    }
    return nomes;
  }

  private record ConteudoInterpretado(TipoMensagem tipo, String conteudo) {}
}
