package com.felipeduan.atendimento.modules.empresas;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.deleteEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.getEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.listarEmpresas;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.listarEmpresasInativas;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class EmpresaInativarIntegrationTest extends AbstractEmpresaIntegrationTest {

  @Test
  void deveInativarEmpresa_quandoPlatformAdmin() throws Exception {
    UUID empresaId = criarEmpresaPadrao();
    String token = obterTokenPlatformAdmin();

    deleteEmpresa(mockMvc, token, empresaId).andExpect(status().isNoContent());

    getEmpresa(mockMvc, token, empresaId).andExpect(status().isNotFound());
  }

  @Test
  void deveRetornar403_quandoAdministradorTentaInativarEmpresa() throws Exception {
    DadosEmpresaCriada dados = criarEmpresaComAdmin();
    liberarSenhaDefinitiva(
        usuarioRepository, passwordEncoder, dados.emailAdmin(), dados.senhaAdmin());
    String token = obterToken(mockMvc, dados.emailAdmin(), dados.senhaAdmin());

    deleteEmpresa(mockMvc, token, dados.empresaId()).andExpect(status().isForbidden());
  }

  @Test
  void deveListarEmpresaInativaAposDelete() throws Exception {
    UUID empresaId = criarEmpresaPadrao();
    String token = obterTokenPlatformAdmin();

    deleteEmpresa(mockMvc, token, empresaId).andExpect(status().isNoContent());

    listarEmpresas(mockMvc, token)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0));

    listarEmpresasInativas(mockMvc, token)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(empresaId.toString()))
        .andExpect(jsonPath("$.content[0].status").value("INATIVA"));
  }
}
