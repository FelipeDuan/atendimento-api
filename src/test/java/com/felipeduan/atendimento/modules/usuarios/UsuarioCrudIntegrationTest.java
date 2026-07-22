package com.felipeduan.atendimento.modules.usuarios;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.cnpjUnico;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoCriarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.postCriarEmpresa;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.EMAIL;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.SENHA;
import static com.felipeduan.atendimento.support.UsuarioHttpSupport.corpoAtualizarUsuario;
import static com.felipeduan.atendimento.support.UsuarioHttpSupport.corpoCriarUsuario;
import static com.felipeduan.atendimento.support.UsuarioHttpSupport.getUsuario;
import static com.felipeduan.atendimento.support.UsuarioHttpSupport.getUsuarios;
import static com.felipeduan.atendimento.support.UsuarioHttpSupport.postUsuario;
import static com.felipeduan.atendimento.support.UsuarioHttpSupport.putUsuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.modules.conversas.AbstractConversaIntegrationTest;
import com.felipeduan.atendimento.support.PlatformAdminTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsuarioCrudIntegrationTest extends AbstractConversaIntegrationTest {

  @Test
  void deveExecutarCrudDeUsuarioNoTenant() throws Exception {
    String email = "atendente-" + UUID.randomUUID() + "@empresa.local";
    String json =
        postUsuario(
                mockMvc,
                cenario.tokenAdmin(),
                corpoCriarUsuario("Atendente Novo", email, "SenhaTemp123!", "ATENDENTE"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.perfil").value("ATENDENTE"))
            .andExpect(jsonPath("$.status").value("ATIVO"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String usuarioId = JsonPath.read(json, "$.id");

    getUsuario(mockMvc, cenario.tokenAdmin(), usuarioId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Atendente Novo"));

    putUsuario(
            mockMvc,
            cenario.tokenAdmin(),
            usuarioId,
            corpoAtualizarUsuario("Atendente Renomeado", "ATENDENTE", "ATIVO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Atendente Renomeado"));

    getUsuarios(mockMvc, cenario.tokenAdmin())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(usuarioId)).exists());
  }

  @Test
  void deveRetornar409_quandoEmailExisteComSenhaDiferente() throws Exception {
    String email = "duplicado-" + UUID.randomUUID() + "@empresa.local";
    postUsuario(
            mockMvc,
            cenario.tokenAdmin(),
            corpoCriarUsuario("Primeiro", email, "SenhaTemp123!", "ATENDENTE"))
        .andExpect(status().isCreated());

    postUsuario(
            mockMvc,
            cenario.tokenAdmin(),
            corpoCriarUsuario("Segundo", email, "OutraSenha99!", "ATENDENTE"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("E-mail já cadastrado"));
  }

  @Test
  void deveVincularUsuarioExistente_quandoSenhaConfereEmOutroTenant() throws Exception {
    String email = "multi-" + UUID.randomUUID() + "@empresa.local";
    String senha = "SenhaTemp123!";
    postUsuario(
            mockMvc,
            cenario.tokenAdmin(),
            corpoCriarUsuario("Multi Tenant", email, senha, "ATENDENTE"))
        .andExpect(status().isCreated());

    String tokenB = criarTokenTenantB();
    postUsuario(mockMvc, tokenB, corpoCriarUsuario("Multi Tenant", email, senha, "ATENDENTE"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email));
  }

  @Test
  void naoDeveVisualizarUsuarioDeOutroTenant() throws Exception {
    String email = "isolado-" + UUID.randomUUID() + "@empresa.local";
    String json =
        postUsuario(
                mockMvc,
                cenario.tokenAdmin(),
                corpoCriarUsuario("Só A", email, "SenhaTemp123!", "ATENDENTE"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String usuarioId = JsonPath.read(json, "$.id");

    String tokenB = criarTokenTenantB();
    getUsuario(mockMvc, tokenB, usuarioId).andExpect(status().isNotFound());
  }

  @Test
  void naoDeveDesativarUltimoAdministradorAtivo() throws Exception {
    String listagem =
        getUsuarios(mockMvc, cenario.tokenAdmin())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<Map<String, Object>> admins =
        JsonPath.read(listagem, "$.content[?(@.perfil == 'ADMINISTRADOR' && @.status == 'ATIVO')]");
    String adminId = admins.getFirst().get("id").toString();

    putUsuario(
            mockMvc,
            cenario.tokenAdmin(),
            adminId,
            corpoAtualizarUsuario("Admin", "ADMINISTRADOR", "INATIVO"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Último administrador"));
  }

  @Test
  void deveListarApenasUsuariosDoTenant() throws Exception {
    String tokenB = criarTokenTenantB();
    postUsuario(
            mockMvc,
            tokenB,
            corpoCriarUsuario(
                "Só B",
                "so-b-" + UUID.randomUUID() + "@empresa.local",
                "SenhaTemp123!",
                "ATENDENTE"))
        .andExpect(status().isCreated());

    String listagemA =
        getUsuarios(mockMvc, cenario.tokenAdmin())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(listagemA).doesNotContain("Só B");
  }

  @Test
  void naoDeveListarUsuarioInativo() throws Exception {
    String email = "inativo-" + UUID.randomUUID() + "@empresa.local";
    String json =
        postUsuario(
                mockMvc,
                cenario.tokenAdmin(),
                corpoCriarUsuario("Inativável", email, "SenhaTemp123!", "ATENDENTE"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String usuarioId = JsonPath.read(json, "$.id");

    putUsuario(
            mockMvc,
            cenario.tokenAdmin(),
            usuarioId,
            corpoAtualizarUsuario("Inativável", "ATENDENTE", "INATIVO"))
        .andExpect(status().isOk());

    getUsuario(mockMvc, cenario.tokenAdmin(), usuarioId).andExpect(status().isNotFound());

    String listagem =
        getUsuarios(mockMvc, cenario.tokenAdmin())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(listagem).doesNotContain(usuarioId);
  }

  @Test
  void deveReativarVinculo_quandoPutDefineStatusAtivo() throws Exception {
    String email = "reativavel-" + UUID.randomUUID() + "@empresa.local";
    String json =
        postUsuario(
                mockMvc,
                cenario.tokenAdmin(),
                corpoCriarUsuario("Reativável", email, "SenhaTemp123!", "ATENDENTE"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String usuarioId = JsonPath.read(json, "$.id");

    putUsuario(
            mockMvc,
            cenario.tokenAdmin(),
            usuarioId,
            corpoAtualizarUsuario("Reativável", "ATENDENTE", "INATIVO"))
        .andExpect(status().isOk());
    getUsuario(mockMvc, cenario.tokenAdmin(), usuarioId).andExpect(status().isNotFound());

    putUsuario(
            mockMvc,
            cenario.tokenAdmin(),
            usuarioId,
            corpoAtualizarUsuario("Reativável", "ATENDENTE", "ATIVO"))
        .andExpect(status().isOk());
    getUsuario(mockMvc, cenario.tokenAdmin(), usuarioId).andExpect(status().isOk());
  }

  @Test
  void deveListarUsuarios_quandoAtendente() throws Exception {
    String tokenAtendente = criarAtendenteEObterToken();
    getUsuarios(mockMvc, tokenAtendente).andExpect(status().isOk());
  }

  @Test
  void deveNegarCriacao_quandoAtendente() throws Exception {
    String tokenAtendente = criarAtendenteEObterToken();
    String senha = "SenhaTemp123!";

    postUsuario(
            mockMvc,
            tokenAtendente,
            corpoCriarUsuario(
                "Bloqueado",
                "bloqueado-" + UUID.randomUUID() + "@empresa.local",
                senha,
                "ATENDENTE"))
        .andExpect(status().isForbidden());
  }

  @Test
  void deveNegarAtualizacao_quandoAtendente() throws Exception {
    String tokenAtendente = criarAtendenteEObterToken();
    String listagem =
        getUsuarios(mockMvc, cenario.tokenAdmin())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<Map<String, Object>> admins =
        JsonPath.read(listagem, "$.content[?(@.perfil == 'ADMINISTRADOR' && @.status == 'ATIVO')]");
    String adminId = admins.getFirst().get("id").toString();

    putUsuario(
            mockMvc, tokenAtendente, adminId, corpoAtualizarUsuario("Hack", "ATENDENTE", "ATIVO"))
        .andExpect(status().isForbidden());
  }

  @Test
  void deveResponderProblemDetail_quandoAtendenteTentaEscrever() throws Exception {
    String email = "atendente-403-" + UUID.randomUUID() + "@empresa.local";
    String senha = "SenhaTemp123!";
    postUsuario(
            mockMvc,
            cenario.tokenAdmin(),
            corpoCriarUsuario("Atendente 403", email, senha, "ATENDENTE"))
        .andExpect(status().isCreated());

    liberarSenhaDefinitiva(usuarioRepository, passwordEncoder, email, senha);
    String tokenAtendente = obterToken(mockMvc, email, senha);

    postUsuario(
            mockMvc,
            tokenAtendente,
            corpoCriarUsuario(
                "Bloqueado", "b-" + UUID.randomUUID() + "@e.local", senha, "ATENDENTE"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.title").value("Acesso negado"));
  }

  private String criarAtendenteEObterToken() throws Exception {
    String email = "atendente-auth-" + UUID.randomUUID() + "@empresa.local";
    String senha = "SenhaTemp123!";
    postUsuario(
            mockMvc,
            cenario.tokenAdmin(),
            corpoCriarUsuario("Atendente Auth", email, senha, "ATENDENTE"))
        .andExpect(status().isCreated());

    liberarSenhaDefinitiva(usuarioRepository, passwordEncoder, email, senha);
    return obterToken(mockMvc, email, senha);
  }

  private String criarTokenTenantB() throws Exception {
    String cnpj = cnpjUnico();
    String emailAdminB = "admin-user-b-" + cnpj + "@empresa.local";
    String senhaAdminB = "SenhaTemp123!";
    String tokenPlatform = PlatformAdminTestSupport.obterToken(mockMvc, EMAIL, SENHA);

    postCriarEmpresa(mockMvc, tokenPlatform, corpoCriarEmpresa(cnpj, emailAdminB, senhaAdminB))
        .andExpect(status().isCreated());

    liberarSenhaDefinitiva(usuarioRepository, passwordEncoder, emailAdminB, senhaAdminB);
    return obterToken(mockMvc, emailAdminB, senhaAdminB);
  }
}
