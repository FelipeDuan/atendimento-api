package com.felipeduan.atendimento.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class ContatoHttpSupport {

  public static final String CONTATOS_PATH = "/contatos";

  private ContatoHttpSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static ResultActions postContato(MockMvc mockMvc, String token, String corpo)
      throws Exception {
    return mockMvc.perform(
        comAutenticacao(
            post(CONTATOS_PATH).contentType(MediaType.APPLICATION_JSON).content(corpo), token));
  }

  public static ResultActions getContatos(MockMvc mockMvc, String token) throws Exception {
    return mockMvc.perform(comAutenticacao(get(CONTATOS_PATH), token));
  }

  public static ResultActions getContato(MockMvc mockMvc, String token, String contatoId)
      throws Exception {
    return mockMvc.perform(comAutenticacao(get(CONTATOS_PATH + "/" + contatoId), token));
  }

  public static ResultActions putContato(
      MockMvc mockMvc, String token, String contatoId, String corpo) throws Exception {
    return mockMvc.perform(
        comAutenticacao(
            put(CONTATOS_PATH + "/" + contatoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo),
            token));
  }

  public static ResultActions deleteContato(MockMvc mockMvc, String token, String contatoId)
      throws Exception {
    return mockMvc.perform(comAutenticacao(delete(CONTATOS_PATH + "/" + contatoId), token));
  }

  public static String corpoCriarContato(String nome, String numeroWhatsapp) {
    return corpoCriarContato(nome, numeroWhatsapp, null, null);
  }

  public static String corpoCriarContato(
      String nome, String numeroWhatsapp, String email, String observacoes) {
    String emailJson = email == null ? "null" : "\"%s\"".formatted(email);
    String observacoesJson = observacoes == null ? "null" : "\"%s\"".formatted(observacoes);
    return """
        {
          "nome": "%s",
          "numeroWhatsapp": "%s",
          "email": %s,
          "observacoes": %s
        }
        """
        .formatted(nome, numeroWhatsapp, emailJson, observacoesJson);
  }

  public static String corpoAtualizarContato(String nome, String email, String observacoes) {
    String emailJson = email == null ? "null" : "\"%s\"".formatted(email);
    String observacoesJson = observacoes == null ? "null" : "\"%s\"".formatted(observacoes);
    return """
        {
          "nome": "%s",
          "email": %s,
          "observacoes": %s
        }
        """
        .formatted(nome, emailJson, observacoesJson);
  }

  private static MockHttpServletRequestBuilder comAutenticacao(
      MockHttpServletRequestBuilder request, String token) {
    if (token != null) {
      request = request.header("Authorization", "Bearer " + token);
    }
    return request;
  }
}
