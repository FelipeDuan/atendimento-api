package com.felipeduan.atendimento.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class AuthHttpSupport {

  private static final String LOGIN_PATH = "/auth/login";
  private static final String TROCAR_SENHA_PATH = "/auth/trocar-senha";
  private static final String SWITCH_TENANT_PATH = "/auth/switch-tenant";

  private AuthHttpSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static ResultActions postLogin(MockMvc mockMvc, String email, String senha)
      throws Exception {
    return mockMvc.perform(
        post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(corpoLogin(email, senha)));
  }

  public static String obterToken(MockMvc mockMvc, String email, String senha) throws Exception {
    String corpo =
        postLogin(mockMvc, email, senha)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(corpo, "$.accessToken");
  }

  public static ResultActions postSwitchTenant(MockMvc mockMvc, String token, UUID empresaId)
      throws Exception {
    return mockMvc.perform(
        post(SWITCH_TENANT_PATH)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"empresaId\":\"%s\"}".formatted(empresaId)));
  }

  public static ResultActions postTrocarSenha(
      MockMvc mockMvc, String token, String senhaAtual, String novaSenha) throws Exception {
    return mockMvc.perform(
        post(TROCAR_SENHA_PATH)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpoTrocarSenha(senhaAtual, novaSenha)));
  }

  public static String corpoLogin(String email, String senha) {
    return """
        {"email":"%s","senha":"%s"}
        """
        .formatted(email, senha);
  }

  public static String corpoTrocarSenha(String senhaAtual, String novaSenha) {
    return """
        {"senhaAtual":"%s","novaSenha":"%s"}
        """
        .formatted(senhaAtual, novaSenha);
  }
}
