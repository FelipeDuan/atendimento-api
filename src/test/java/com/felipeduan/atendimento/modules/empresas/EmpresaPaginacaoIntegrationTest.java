package com.felipeduan.atendimento.modules.empresas;

import static com.felipeduan.atendimento.support.EmpresaHttpSupport.EMPRESAS_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class EmpresaPaginacaoIntegrationTest extends AbstractEmpresaIntegrationTest {

  @Test
  void deveRetornarSegundaPagina_quandoExistemMaisEmpresasQueOTamanhoDaPagina() throws Exception {
    criarEmpresaPadrao();
    criarEmpresaPadrao();
    criarEmpresaPadrao();
    String token = obterTokenPlatformAdmin();

    mockMvc
        .perform(get(EMPRESAS_PATH + "?page=1&size=2").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.first").value(false))
        .andExpect(jsonPath("$.last").value(true));
  }

  @Test
  void deveLimitarTamanhoDaPagina_quandoClienteSolicitaAcimaDoMaximo() throws Exception {
    criarEmpresaPadrao();
    String token = obterTokenPlatformAdmin();

    mockMvc
        .perform(get(EMPRESAS_PATH + "?size=5000").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(100));
  }

  @Test
  void deveOrdenarInativasPorPropriedadeDaEntidade_quandoClienteInformaSort() throws Exception {
    String token = obterTokenPlatformAdmin();

    mockMvc
        .perform(
            get(EMPRESAS_PATH + "/inativas?sort=dataCriacao,desc")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void deveOrdenarAtivasPorNome_quandoClienteInformaSort() throws Exception {
    criarEmpresaPadrao();
    String token = obterTokenPlatformAdmin();

    mockMvc
        .perform(get(EMPRESAS_PATH + "?sort=nome,asc").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }
}
