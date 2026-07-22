package com.felipeduan.atendimento.modules.webhook;

import com.felipeduan.atendimento.shared.config.MetaProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/messages")
@Tag(name = "Webhook")
@RequiredArgsConstructor
public class WebhookController {

  private final AssinaturaWebhook assinaturaWebhook;
  private final WebhookService webhookService;
  private final MetaProperties metaProperties;

  @GetMapping
  @Operation(operationId = "verificarWebhook")
  public ResponseEntity<String> verificar(
      @RequestParam("hub.mode") String modo,
      @RequestParam("hub.verify_token") String token,
      @RequestParam("hub.challenge") String challenge) {

    if (!"subscribe".equals(modo) || !metaProperties.verifyToken().equals(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    return ResponseEntity.ok(challenge);
  }

  @PostMapping
  @Operation(operationId = "receberWebhook")
  public ResponseEntity<Void> receber(
      @RequestHeader(name = "X-Hub-Signature-256", required = false) String assinatura,
      @RequestBody byte[] corpoBruto) {

    assinaturaWebhook.validar(corpoBruto, assinatura);
    webhookService.processar(corpoBruto);
    return ResponseEntity.ok().build();
  }
}
