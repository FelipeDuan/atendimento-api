package com.felipeduan.atendimento.modules.webhook;

import com.felipeduan.atendimento.modules.webhook.exception.AssinaturaWebhookInvalidaException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AssinaturaWebhook {

  private static final String PREFIXO = "sha256=";
  private static final String ALGORITMO = "HmacSHA256";

  private final SecretKeySpec chave;

  public AssinaturaWebhook(@Value("${meta.app-secret}") String appSecret) {
    this.chave = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), ALGORITMO);
  }

  public void validar(byte[] corpoBruto, String cabecalho) {
    if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
      throw new AssinaturaWebhookInvalidaException();
    }

    byte[] esperada = calcular(corpoBruto);
    byte[] recebida = decodificar(cabecalho.substring(PREFIXO.length()));

    if (!MessageDigest.isEqual(esperada, recebida)) {
      throw new AssinaturaWebhookInvalidaException();
    }
  }

  public String assinar(byte[] corpoBruto) {
    return PREFIXO + HexFormat.of().formatHex(calcular(corpoBruto));
  }

  private byte[] calcular(byte[] corpoBruto) {
    try {
      Mac mac = Mac.getInstance(ALGORITMO);
      mac.init(chave);
      return mac.doFinal(corpoBruto);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Falha ao calcular a assinatura do webhook", e);
    }
  }

  private byte[] decodificar(String hexadecimal) {
    try {
      return HexFormat.of().parseHex(hexadecimal);
    } catch (IllegalArgumentException e) {
      throw new AssinaturaWebhookInvalidaException();
    }
  }
}
