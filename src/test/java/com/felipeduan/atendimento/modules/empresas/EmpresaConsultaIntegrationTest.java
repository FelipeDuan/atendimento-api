package com.felipeduan.atendimento.modules.empresas;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.getEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.listarEmpresas;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.listarEmpresasInativas;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class EmpresaConsultaIntegrationTest extends AbstractEmpresaIntegrationTest {

  @Test
  void deveRetornarEmpresa_quandoPlatformAdminConsulta() throws Exception {
    UUID empresaId = criarEmpresaPadrao();
    String token = obterTokenPlatformAdmin();

    getEmpresa(mockMvc, token, empresaId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(empresaId.toString()))
        .andExpect(jsonPath("$.status").value("ATIVA"));
  }

  @Test
  void deveRetornarEmpresa_quandoAdministradorConsultaPropriaEmpresa() throws Exception {
    DadosEmpresaCriada dados = criarEmpresaComAdmin();
    liberarSenhaDefinitiva(
        usuarioRepository, passwordEncoder, dados.emailAdmin(), dados.senhaAdmin());
    String token = obterToken(mockMvc, dados.emailAdmin(), dados.senhaAdmin());

    getEmpresa(mockMvc, token, dados.empresaId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(dados.empresaId().toString()));
  }

  @Test
  void deveRetornar403_quandoAdministradorConsultaOutraEmpresa() throws Exception {
    DadosEmpresaCriada dados = criarEmpresaComAdmin();
    liberarSenhaDefinitiva(
        usuarioRepository, passwordEncoder, dados.emailAdmin(), dados.senhaAdmin());
    String token = obterToken(mockMvc, dados.emailAdmin(), dados.senhaAdmin());

    getEmpresa(mockMvc, token, UUID.randomUUID()).andExpect(status().isForbidden());
  }

  @Test
  void deveListarEmpresasAtivas_quandoPlatformAdmin() throws Exception {
    UUID empresaId = criarEmpresaPadrao();
    String token = obterTokenPlatformAdmin();

    listarEmpresas(mockMvc, token)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(empresaId.toString()))
        .andExpect(jsonPath("$.content[0].status").value("ATIVA"));
  }

  @Test
  void deveRetornar403_quandoAdministradorListaEmpresas() throws Exception {
    DadosEmpresaCriada dados = criarEmpresaComAdmin();
    liberarSenhaDefinitiva(
        usuarioRepository, passwordEncoder, dados.emailAdmin(), dados.senhaAdmin());
    String token = obterToken(mockMvc, dados.emailAdmin(), dados.senhaAdmin());

    listarEmpresas(mockMvc, token).andExpect(status().isForbidden());
    listarEmpresasInativas(mockMvc, token).andExpect(status().isForbidden());
  }
}
