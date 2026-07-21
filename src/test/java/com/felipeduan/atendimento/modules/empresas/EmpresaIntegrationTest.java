package com.felipeduan.atendimento.modules.empresas;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.cnpjUnico;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoAtualizarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoCriarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.deleteEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.getEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.postCriarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.putEmpresa;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.EMAIL;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.NOME;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.SENHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.AbstractIntegrationTest;
import com.felipeduan.atendimento.modules.platformadmin.AdministradorPlataformaRepository;
import com.felipeduan.atendimento.modules.usuarios.UsuarioRepository;
import com.felipeduan.atendimento.modules.vinculos.UsuarioEmpresaRepository;
import com.felipeduan.atendimento.shared.security.JwtService;
import com.felipeduan.atendimento.shared.security.Roles;
import com.felipeduan.atendimento.support.LimpezaDadosTestSupport;
import com.felipeduan.atendimento.support.PlatformAdminTestSupport;
import com.felipeduan.atendimento.support.RlsTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfigureMockMvc
class EmpresaIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired JwtService jwtService;
  @Autowired AdministradorPlataformaRepository administradorPlataformaRepository;
  @Autowired EmpresaRepository empresaRepository;
  @Autowired UsuarioRepository usuarioRepository;
  @Autowired UsuarioEmpresaRepository usuarioEmpresaRepository;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired EntityManager entityManager;

  @BeforeEach
  void prepararDados() {
    LimpezaDadosTestSupport.limparDadosNegocio(
        empresaRepository, usuarioRepository, entityManager, transactionTemplate);
    administradorPlataformaRepository.deleteAll();
    PlatformAdminTestSupport.salvarAdministrador(
        administradorPlataformaRepository, passwordEncoder, NOME, EMAIL, SENHA);
  }

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
    assertThat(usuario.getLastEmpresaId()).isEqualTo(empresaIdSegunda);

    assertThat(
            RlsTestSupport.contarVinculosDaEmpresa(
                empresaIdPrimeira, usuarioEmpresaRepository, entityManager, transactionTemplate))
        .isEqualTo(1);
    assertThat(
            RlsTestSupport.contarVinculosDaEmpresa(
                empresaIdSegunda, usuarioEmpresaRepository, entityManager, transactionTemplate))
        .isEqualTo(1);
  }

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

  private UUID criarEmpresaPadrao() throws Exception {
    return criarEmpresaComAdmin().empresaId();
  }

  private DadosEmpresaCriada criarEmpresaComAdmin() throws Exception {
    String cnpj = cnpjUnico();
    String emailAdmin = "admin-" + cnpj + "@empresa.local";
    String senha = "SenhaTemp123!";
    String token = obterTokenPlatformAdmin();

    String json =
        postCriarEmpresa(mockMvc, token, corpoCriarEmpresa(cnpj, emailAdmin, senha))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID empresaId = UUID.fromString(JsonPath.read(json, "$.id"));
    return new DadosEmpresaCriada(empresaId, emailAdmin, senha);
  }

  private String obterTokenPlatformAdmin() throws Exception {
    return PlatformAdminTestSupport.obterToken(mockMvc, EMAIL, SENHA);
  }

  private record DadosEmpresaCriada(UUID empresaId, String emailAdmin, String senhaAdmin) {}
}
