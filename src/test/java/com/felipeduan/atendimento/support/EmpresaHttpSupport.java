package com.felipeduan.atendimento.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class EmpresaHttpSupport {

  public static final String EMPRESAS_PATH = "/empresas";

  private EmpresaHttpSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static ResultActions postCriarEmpresa(MockMvc mockMvc, String token, String corpo)
      throws Exception {
    var request = post(EMPRESAS_PATH).contentType(MediaType.APPLICATION_JSON).content(corpo);

    if (token != null) {
      request = request.header("Authorization", "Bearer " + token);
    }

    return mockMvc.perform(request);
  }

  public static String corpoCriarEmpresa(String cnpj, String emailAdmin, String senha) {
    return """
        {
          "nome": "Empresa Teste",
          "cnpj": "%s",
          "email": "contato@empresa.local",
          "administradorInicial": {
            "nome": "Admin Teste",
            "email": "%s",
            "senhaTemporaria": "%s"
          }
        }
        """
        .formatted(cnpj, emailAdmin, senha);
  }

  public static String cnpjUnico() {
    return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 14);
  }
}
