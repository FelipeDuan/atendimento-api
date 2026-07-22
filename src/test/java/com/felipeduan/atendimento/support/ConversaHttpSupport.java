package com.felipeduan.atendimento.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class ConversaHttpSupport {

  public static final String CONVERSAS_PATH = "/conversas";

  private ConversaHttpSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static ResultActions getConversas(MockMvc mockMvc, String token) throws Exception {
    return mockMvc.perform(comAutenticacao(get(CONVERSAS_PATH), token));
  }

  public static ResultActions getConversa(MockMvc mockMvc, String token, String conversaId)
      throws Exception {
    return mockMvc.perform(comAutenticacao(get(CONVERSAS_PATH + "/" + conversaId), token));
  }

  public static ResultActions patchConversa(
      MockMvc mockMvc, String token, String conversaId, String acao) throws Exception {
    return mockMvc.perform(
        comAutenticacao(
            patch(CONVERSAS_PATH + "/" + conversaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\":\"" + acao + "\"}"),
            token));
  }

  public static ResultActions getMensagens(MockMvc mockMvc, String token, String conversaId)
      throws Exception {
    return mockMvc.perform(
        comAutenticacao(get(CONVERSAS_PATH + "/" + conversaId + "/mensagens"), token));
  }

  public static ResultActions postMensagem(
      MockMvc mockMvc, String token, String conversaId, String conteudo) throws Exception {
    return mockMvc.perform(
        comAutenticacao(
            post(CONVERSAS_PATH + "/" + conversaId + "/mensagens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"tipo":"TEXTO","conteudo":"%s"}
                    """
                        .formatted(conteudo)),
            token));
  }

  private static MockHttpServletRequestBuilder comAutenticacao(
      MockHttpServletRequestBuilder request, String token) {
    if (token != null) {
      request = request.header("Authorization", "Bearer " + token);
    }
    return request;
  }
}
