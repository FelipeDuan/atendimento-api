package com.felipeduan.atendimento.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.felipeduan.atendimento.modules.webhook.AssinaturaWebhook;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class WebhookHttpSupport {

  public static final String WEBHOOK_PATH = "/webhooks/messages";

  private WebhookHttpSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static ResultActions getVerificacao(
      MockMvc mockMvc, String mode, String token, String challenge) throws Exception {
    return mockMvc.perform(
        get(WEBHOOK_PATH)
            .param("hub.mode", mode)
            .param("hub.verify_token", token)
            .param("hub.challenge", challenge));
  }

  public static ResultActions postWebhook(
      MockMvc mockMvc, AssinaturaWebhook assinatura, String corpoJson) throws Exception {
    byte[] corpo = corpoJson.getBytes(StandardCharsets.UTF_8);
    return mockMvc.perform(
        post(WEBHOOK_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Hub-Signature-256", assinatura.assinar(corpo))
            .content(corpo));
  }

  public static ResultActions postWebhookComAssinatura(
      MockMvc mockMvc, String assinatura, String corpoJson) throws Exception {
    return mockMvc.perform(
        post(WEBHOOK_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Hub-Signature-256", assinatura)
            .content(corpoJson.getBytes(StandardCharsets.UTF_8)));
  }

  public static ResultActions postWebhookSemAssinatura(MockMvc mockMvc, String corpoJson)
      throws Exception {
    return mockMvc.perform(
        post(WEBHOOK_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpoJson.getBytes(StandardCharsets.UTF_8)));
  }

  public static String corpoMensagemTexto(
      String phoneNumberId, String waId, String nome, String wamid, String texto) {
    return """
        {
          "entry": [{
            "changes": [{
              "value": {
                "metadata": { "phone_number_id": "%s" },
                "contacts": [{ "wa_id": "%s", "profile": { "name": "%s" } }],
                "messages": [{
                  "id": "%s",
                  "from": "%s",
                  "type": "text",
                  "text": { "body": "%s" }
                }]
              }
            }]
          }]
        }
        """
        .formatted(phoneNumberId, waId, nome, wamid, waId, texto);
  }

  public static String corpoMensagemImagem(
      String phoneNumberId, String waId, String nome, String wamid, String caption) {
    return """
        {
          "entry": [{
            "changes": [{
              "value": {
                "metadata": { "phone_number_id": "%s" },
                "contacts": [{ "wa_id": "%s", "profile": { "name": "%s" } }],
                "messages": [{
                  "id": "%s",
                  "from": "%s",
                  "type": "image",
                  "image": { "id": "media.img.1", "caption": "%s", "mime_type": "image/jpeg" }
                }]
              }
            }]
          }]
        }
        """
        .formatted(phoneNumberId, waId, nome, wamid, waId, caption);
  }
}
