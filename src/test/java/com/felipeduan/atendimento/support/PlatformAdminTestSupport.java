package com.felipeduan.atendimento.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.modules.platformadmin.AdministradorPlataforma;
import com.felipeduan.atendimento.modules.platformadmin.AdministradorPlataformaRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class PlatformAdminTestSupport {

  private static final String LOGIN_PATH = "/auth/plataforma/login";

  private PlatformAdminTestSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static void salvarAdministrador(
      AdministradorPlataformaRepository repository,
      PasswordEncoder passwordEncoder,
      String nome,
      String email,
      String senha) {
    var administrador = new AdministradorPlataforma(nome, email, passwordEncoder.encode(senha));
    repository.save(administrador);
  }

  public static String obterToken(MockMvc mockMvc, String email, String senha) throws Exception {
    ResultActions resposta =
        mockMvc.perform(
            post(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoLogin(email, senha)));

    String corpo =
        resposta.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return JsonPath.read(corpo, "$.accessToken");
  }

  public static String corpoLogin(String email, String senha) {
    return """
        {"email":"%s","senha":"%s"}
        """
        .formatted(email, senha);
  }
}
