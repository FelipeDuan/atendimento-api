package com.felipeduan.atendimento.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class UsuarioHttpSupport {

  public static final String USUARIOS_PATH = "/usuarios";

  private UsuarioHttpSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static ResultActions postUsuario(MockMvc mockMvc, String token, String corpo)
      throws Exception {
    return mockMvc.perform(
        comAutenticacao(
            post(USUARIOS_PATH).contentType(MediaType.APPLICATION_JSON).content(corpo), token));
  }

  public static ResultActions getUsuarios(MockMvc mockMvc, String token) throws Exception {
    return mockMvc.perform(comAutenticacao(get(USUARIOS_PATH), token));
  }

  public static ResultActions getUsuario(MockMvc mockMvc, String token, String usuarioId)
      throws Exception {
    return mockMvc.perform(comAutenticacao(get(USUARIOS_PATH + "/" + usuarioId), token));
  }

  public static ResultActions putUsuario(
      MockMvc mockMvc, String token, String usuarioId, String corpo) throws Exception {
    return mockMvc.perform(
        comAutenticacao(
            put(USUARIOS_PATH + "/" + usuarioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo),
            token));
  }

  public static String corpoCriarUsuario(String nome, String email, String senha, String perfil) {
    return """
        {
          "nome": "%s",
          "email": "%s",
          "senha": "%s",
          "perfil": "%s"
        }
        """
        .formatted(nome, email, senha, perfil);
  }

  public static String corpoAtualizarUsuario(String nome, String perfil, String status) {
    return """
        {
          "nome": "%s",
          "perfil": "%s",
          "status": "%s"
        }
        """
        .formatted(nome, perfil, status);
  }

  private static MockHttpServletRequestBuilder comAutenticacao(
      MockHttpServletRequestBuilder request, String token) {
    if (token != null) {
      request = request.header("Authorization", "Bearer " + token);
    }
    return request;
  }
}
