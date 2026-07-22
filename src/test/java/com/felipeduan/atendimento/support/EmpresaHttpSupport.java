package com.felipeduan.atendimento.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class EmpresaHttpSupport {

  public static final String EMPRESAS_PATH = "/empresas";

  private EmpresaHttpSupport() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static ResultActions postCriarEmpresa(MockMvc mockMvc, String token, String corpo)
      throws Exception {
    return mockMvc.perform(
        comAutenticacao(
            post(EMPRESAS_PATH).contentType(MediaType.APPLICATION_JSON).content(corpo), token));
  }

  public static ResultActions getEmpresa(MockMvc mockMvc, String token, UUID empresaId)
      throws Exception {
    return mockMvc.perform(comAutenticacao(get(EMPRESAS_PATH + "/" + empresaId), token));
  }

  public static ResultActions listarEmpresas(MockMvc mockMvc, String token) throws Exception {
    return mockMvc.perform(comAutenticacao(get(EMPRESAS_PATH), token));
  }

  public static ResultActions listarEmpresasInativas(MockMvc mockMvc, String token)
      throws Exception {
    return mockMvc.perform(comAutenticacao(get(EMPRESAS_PATH + "/inativas"), token));
  }

  public static ResultActions putEmpresa(
      MockMvc mockMvc, String token, UUID empresaId, String corpo) throws Exception {
    return mockMvc.perform(
        comAutenticacao(
            put(EMPRESAS_PATH + "/" + empresaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo),
            token));
  }

  public static ResultActions deleteEmpresa(MockMvc mockMvc, String token, UUID empresaId)
      throws Exception {
    return mockMvc.perform(comAutenticacao(delete(EMPRESAS_PATH + "/" + empresaId), token));
  }

  public static String corpoAtualizarEmpresa(String nome, String email, String phoneNumberId) {
    String phoneNumberJson = phoneNumberId == null ? "null" : "\"%s\"".formatted(phoneNumberId);

    return """
        {
          "nome": "%s",
          "email": "%s",
          "phoneNumberId": %s
        }
        """
        .formatted(nome, email, phoneNumberJson);
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
    return UUID.randomUUID().toString().replace("-", "").substring(0, 14);
  }

  private static MockHttpServletRequestBuilder comAutenticacao(
      MockHttpServletRequestBuilder request, String token) {

    if (token != null) {
      request = request.header("Authorization", "Bearer " + token);
    }

    return request;
  }
}
