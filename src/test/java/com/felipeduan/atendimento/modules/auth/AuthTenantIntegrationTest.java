package com.felipeduan.atendimento.modules.auth;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthHttpSupport.postLogin;
import static com.felipeduan.atendimento.support.AuthHttpSupport.postSwitchTenant;
import static com.felipeduan.atendimento.support.AuthHttpSupport.postTrocarSenha;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.cnpjUnico;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoCriarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.postCriarEmpresa;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.EMAIL;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.NOME;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.SENHA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.AbstractIntegrationTest;
import com.felipeduan.atendimento.modules.empresas.EmpresaRepository;
import com.felipeduan.atendimento.modules.platformadmin.AdministradorPlataformaRepository;
import com.felipeduan.atendimento.modules.usuarios.UsuarioRepository;
import com.felipeduan.atendimento.shared.security.JwtService;
import com.felipeduan.atendimento.shared.security.Roles;
import com.felipeduan.atendimento.support.LimpezaDadosTestSupport;
import com.felipeduan.atendimento.support.PlatformAdminTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfigureMockMvc
class AuthTenantIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired JwtDecoder jwtDecoder;
  @Autowired AdministradorPlataformaRepository administradorPlataformaRepository;
  @Autowired UsuarioRepository usuarioRepository;
  @Autowired EmpresaRepository empresaRepository;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired EntityManager entityManager;
  @Autowired TransactionTemplate transactionTemplate;

  String emailAdmin;
  String senhaAdmin;
  UUID empresaId1;

  @BeforeEach
  void prepararDados() throws Exception {
    LimpezaDadosTestSupport.limparDadosNegocio(
        empresaRepository, usuarioRepository, entityManager, transactionTemplate);

    administradorPlataformaRepository.deleteAll();

    PlatformAdminTestSupport.salvarAdministrador(
        administradorPlataformaRepository, passwordEncoder, NOME, EMAIL, SENHA);

    String cnpj = cnpjUnico();
    emailAdmin = "admin-" + cnpj + "@empresa.local";
    senhaAdmin = "SenhaTemp123!";

    String tokenPlatform = PlatformAdminTestSupport.obterToken(mockMvc, EMAIL, SENHA);
    String jsonPrimeiraEmpresa =
        postCriarEmpresa(mockMvc, tokenPlatform, corpoCriarEmpresa(cnpj, emailAdmin, senhaAdmin))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    empresaId1 = UUID.fromString(JsonPath.read(jsonPrimeiraEmpresa, "$.id"));
  }

  @Test
  void deveEmitirTokenTrocarSenha_quandoAdminInicial() throws Exception {
    String json =
        postLogin(mockMvc, emailAdmin, senhaAdmin)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exigeTrocarSenha").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

    var jwt = jwtDecoder.decode(JsonPath.read(json, "$.accessToken"));

    assertThat(jwt.getClaimAsStringList(JwtService.CLAIM_AUTHORITIES))
        .containsExactly(Roles.TROCAR_SENHA);
    assertThat(jwt.hasClaim(JwtService.CLAIM_TENANT_ID)).isFalse();
  }

  @Test
  void deveRetornar401_quandoSenhaInvalida() throws Exception {
    postLogin(mockMvc, emailAdmin, "errada")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Credenciais Inválidas"));
  }

  @Test
  void deveEmitirTokenComTenant_aposTrocarSenha() throws Exception {
    String tokenRestrito =
        JsonPath.read(
            postLogin(mockMvc, emailAdmin, senhaAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exigeTrocarSenha").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accessToken");

    String novaSenha = "NovaSenha456!";

    String json =
        postTrocarSenha(mockMvc, tokenRestrito, senhaAdmin, novaSenha)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exigeTrocarSenha").value(false))
            .andExpect(jsonPath("$.empresasVinculadas.length()").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();

    var jwt = jwtDecoder.decode(JsonPath.read(json, "$.accessToken"));

    assertThat(jwt.getClaimAsStringList(JwtService.CLAIM_AUTHORITIES))
        .containsExactly("ADMINISTRADOR");
    assertThat(jwt.getClaimAsString(JwtService.CLAIM_TENANT_ID)).isEqualTo(empresaId1.toString());

    String tokenPosTroca = obterToken(mockMvc, emailAdmin, novaSenha);
    assertThat(jwtDecoder.decode(tokenPosTroca).hasClaim(JwtService.CLAIM_TENANT_ID)).isTrue();
  }

  @Test
  void deveRetornar401_quandoSenhaAtualInvalidaNoTrocarSenha() throws Exception {
    String tokenRestrito =
        JsonPath.read(
            postLogin(mockMvc, emailAdmin, senhaAdmin)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accessToken");

    postTrocarSenha(mockMvc, tokenRestrito, "errada", "NovaSenha456!")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Credenciais Inválidas"));
  }

  @Test
  void deveRetornar403_quandoTokenNormalAcessaTrocarSenha() throws Exception {
    var usuario = usuarioRepository.findByEmail(emailAdmin).orElseThrow();
    usuario.alterarSenha(passwordEncoder.encode(senhaAdmin));
    usuarioRepository.save(usuario);

    String tokenNormal = obterToken(mockMvc, emailAdmin, senhaAdmin);

    postTrocarSenha(mockMvc, tokenNormal, senhaAdmin, "OutraSenha789!")
        .andExpect(status().isForbidden());
  }

  @Test
  void deveRetornar403_quandoTokenRestritoAcessaSwitchTenant() throws Exception {
    String tokenRestrito =
        JsonPath.read(
            postLogin(mockMvc, emailAdmin, senhaAdmin)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accessToken");

    postSwitchTenant(mockMvc, tokenRestrito, empresaId1).andExpect(status().isForbidden());
  }

  @Test
  void deveTrocarTenant_quandoUsuarioTemDoisVinculos() throws Exception {
    String cnpj2 = cnpjUnico();
    String tokenPlatform = PlatformAdminTestSupport.obterToken(mockMvc, EMAIL, SENHA);
    String jsonSegunda =
        postCriarEmpresa(mockMvc, tokenPlatform, corpoCriarEmpresa(cnpj2, emailAdmin, senhaAdmin))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID empresaId2 = UUID.fromString(JsonPath.read(jsonSegunda, "$.id"));

    var usuario = usuarioRepository.findByEmail(emailAdmin).orElseThrow();
    usuario.alterarSenha(passwordEncoder.encode(senhaAdmin));
    usuario.registrarNovoVinculo(empresaId1);
    usuarioRepository.save(usuario);

    String tokenLogin = obterToken(mockMvc, emailAdmin, senhaAdmin);
    UUID tenantLogin =
        UUID.fromString(jwtDecoder.decode(tokenLogin).getClaimAsString(JwtService.CLAIM_TENANT_ID));

    assertThat(tenantLogin).isEqualTo(empresaId1);

    String jsonSwitch =
        postSwitchTenant(mockMvc, tokenLogin, empresaId2)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID tenantNovo =
        UUID.fromString(
            jwtDecoder
                .decode(JsonPath.read(jsonSwitch, "$.accessToken"))
                .getClaimAsString(JwtService.CLAIM_TENANT_ID));

    assertThat(tenantNovo).isEqualTo(empresaId2);
    assertThat(tenantNovo).isNotEqualTo(tenantLogin);
  }
}
