package com.felipeduan.atendimento.modules.empresas;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoAtualizarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.putEmpresa;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class EmpresaAtualizarIntegrationTest extends AbstractEmpresaIntegrationTest {

  @Test
  void deveAtualizarEmpresa_quandoAdministradorDaPropriaEmpresa() throws Exception {
    DadosEmpresaCriada dados = criarEmpresaComAdmin();
    liberarSenhaDefinitiva(
        usuarioRepository, passwordEncoder, dados.emailAdmin(), dados.senhaAdmin());
    String token = obterToken(mockMvc, dados.emailAdmin(), dados.senhaAdmin());

    putEmpresa(
            mockMvc,
            token,
            dados.empresaId(),
            corpoAtualizarEmpresa("Empresa Atualizada", "novo@empresa.local", "phone-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Empresa Atualizada"))
        .andExpect(jsonPath("$.email").value("novo@empresa.local"))
        .andExpect(jsonPath("$.phoneNumberId").value("phone-123"));
  }
}
