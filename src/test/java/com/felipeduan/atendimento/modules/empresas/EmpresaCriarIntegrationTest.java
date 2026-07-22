package com.felipeduan.atendimento.modules.empresas;

import static com.felipeduan.atendimento.support.EmpresaHttpSupport.cnpjUnico;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoCriarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.postCriarEmpresa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.modules.vinculos.UsuarioEmpresaRepository;
import com.felipeduan.atendimento.shared.security.JwtService;
import com.felipeduan.atendimento.shared.security.Roles;
import com.felipeduan.atendimento.support.RlsTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
class EmpresaCriarIntegrationTest extends AbstractEmpresaIntegrationTest {

  @Autowired JwtService jwtService;
  @Autowired UsuarioEmpresaRepository usuarioEmpresaRepository;

  @Test
  void deveRetornar401_semToken() throws Exception {
    String corpo = corpoCriarEmpresa(cnpjUnico(), "admin@empresa.local", "SenhaTemp123!");

    postCriarEmpresa(mockMvc, null, corpo).andExpect(status().isUnauthorized());
  }

  @Test
  void deveRetornar403_quandoTokenSemPlatformAdmin() throws Exception {
    String token =
        jwtService.emitirToken(
            UUID.randomUUID().toString(), List.of(Roles.ADMINISTRADOR), UUID.randomUUID());
    String corpo = corpoCriarEmpresa(cnpjUnico(), "admin@empresa.local", "SenhaTemp123!");

    postCriarEmpresa(mockMvc, token, corpo).andExpect(status().isForbidden());
  }

  @Test
  void deveCriarEmpresa_comPlatformAdmin() throws Exception {
    String cnpj = cnpjUnico();
    String emailAdmin = "admin-" + cnpj + "@empresa.local";
    String senha = "SenhaTemp123!";
    String token = obterTokenPlatformAdmin();
    String corpo = corpoCriarEmpresa(cnpj, emailAdmin, senha);

    ResultActions resposta = postCriarEmpresa(mockMvc, token, corpo);

    String json = resposta.andReturn().getResponse().getContentAsString();
    UUID empresaId = UUID.fromString(JsonPath.read(json, "$.id"));
    UUID usuarioId = UUID.fromString(JsonPath.read(json, "$.administradorInicial.usuarioId"));

    resposta
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("Empresa Teste"))
        .andExpect(jsonPath("$.cnpj").value(cnpj))
        .andExpect(jsonPath("$.status").value("ATIVA"))
        .andExpect(jsonPath("$.administradorInicial.email").value(emailAdmin))
        .andExpect(jsonPath("$.administradorInicial.deveTrocarSenha").value(true));

    assertThat(empresaRepository.findById(empresaId)).isPresent();
    assertThat(usuarioRepository.findById(usuarioId)).isPresent();
    assertThat(
            RlsTestSupport.contarVinculosDaEmpresa(
                empresaId, usuarioEmpresaRepository, entityManager, transactionTemplate))
        .isEqualTo(1);
  }

  @Test
  void deveRetornar400_quandoSenhaTemporariaCurta() throws Exception {
    String token = obterTokenPlatformAdmin();
    String cnpj = cnpjUnico();
    String corpo = corpoCriarEmpresa(cnpj, "admin-" + cnpj + "@empresa.local", "123123");

    postCriarEmpresa(mockMvc, token, corpo)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Dados inválidos"))
        .andExpect(
            jsonPath("$.errors[?(@.campo == 'administradorInicial.senhaTemporaria')].mensagem")
                .value("A senha temporária deve ter no mínimo 8 caracteres"));
  }

  @Test
  void deveRetornar409_quandoCnpjDuplicado() throws Exception {
    String token = obterTokenPlatformAdmin();
    String cnpj = cnpjUnico();

    postCriarEmpresa(
            mockMvc,
            token,
            corpoCriarEmpresa(cnpj, "primeiro-" + cnpj + "@empresa.local", "SenhaTemp123!"))
        .andExpect(status().isCreated());

    postCriarEmpresa(
            mockMvc,
            token,
            corpoCriarEmpresa(cnpj, "segundo-" + cnpj + "@empresa.local", "SenhaTemp123!"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("CNPJ já cadastrado"));
  }

  @Test
  void deveRetornar409_quandoEmailExisteComSenhaErrada() throws Exception {
    String token = obterTokenPlatformAdmin();
    String cnpj1 = cnpjUnico();
    String cnpj2 = cnpjUnico();
    String emailAdmin = "compartilhado-" + cnpj1 + "@empresa.local";

    postCriarEmpresa(mockMvc, token, corpoCriarEmpresa(cnpj1, emailAdmin, "SenhaTemp123!"))
        .andExpect(status().isCreated());

    postCriarEmpresa(mockMvc, token, corpoCriarEmpresa(cnpj2, emailAdmin, "SenhaErrada123!"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("E-mail já cadastrado"));

    assertThat(empresaRepository.count()).isEqualTo(1);
    assertThat(usuarioRepository.count()).isEqualTo(1);
  }

  @Test
  void deveReutilizarContaExistente_quandoEmailESenhaCorretos() throws Exception {
    String token = obterTokenPlatformAdmin();
    String cnpj1 = cnpjUnico();
    String cnpj2 = cnpjUnico();
    String emailAdmin = "reutilizado-" + cnpj1 + "@empresa.local";
    String senha = "SenhaTemp123!";

    String jsonPrimeiraEmpresa =
        postCriarEmpresa(mockMvc, token, corpoCriarEmpresa(cnpj1, emailAdmin, senha))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID usuarioIdPrimeira =
        UUID.fromString(JsonPath.read(jsonPrimeiraEmpresa, "$.administradorInicial.usuarioId"));
    UUID empresaIdPrimeira = UUID.fromString(JsonPath.read(jsonPrimeiraEmpresa, "$.id"));

    String jsonSegundaEmpresa =
        postCriarEmpresa(mockMvc, token, corpoCriarEmpresa(cnpj2, emailAdmin, senha))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.administradorInicial.deveTrocarSenha").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID usuarioIdSegunda =
        UUID.fromString(JsonPath.read(jsonSegundaEmpresa, "$.administradorInicial.usuarioId"));
    UUID empresaIdSegunda = UUID.fromString(JsonPath.read(jsonSegundaEmpresa, "$.id"));

    assertThat(usuarioIdSegunda).isEqualTo(usuarioIdPrimeira);
    assertThat(empresaRepository.count()).isEqualTo(2);
    assertThat(usuarioRepository.count()).isEqualTo(1);

    var usuario = usuarioRepository.findById(usuarioIdPrimeira).orElseThrow();
    assertThat(usuario.isDeveTrocarSenha()).isTrue();
    assertThat(usuario.getLastEmpresaId()).isEqualTo(empresaIdPrimeira);

    assertThat(
            RlsTestSupport.contarVinculosDaEmpresa(
                empresaIdPrimeira, usuarioEmpresaRepository, entityManager, transactionTemplate))
        .isEqualTo(1);
    assertThat(
            RlsTestSupport.contarVinculosDaEmpresa(
                empresaIdSegunda, usuarioEmpresaRepository, entityManager, transactionTemplate))
        .isEqualTo(1);
  }
}
