package com.felipeduan.atendimento.modules.empresas;

import static com.felipeduan.atendimento.support.EmpresaHttpSupport.cnpjUnico;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoCriarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.postCriarEmpresa;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.EMAIL;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.NOME;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.SENHA;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.AbstractIntegrationTest;
import com.felipeduan.atendimento.modules.platformadmin.AdministradorPlataformaRepository;
import com.felipeduan.atendimento.modules.usuarios.UsuarioRepository;
import com.felipeduan.atendimento.support.LimpezaDadosTestSupport;
import com.felipeduan.atendimento.support.PlatformAdminTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class AbstractEmpresaIntegrationTest extends AbstractIntegrationTest {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected AdministradorPlataformaRepository administradorPlataformaRepository;
  @Autowired protected EmpresaRepository empresaRepository;
  @Autowired protected UsuarioRepository usuarioRepository;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected EntityManager entityManager;
  @Autowired protected TransactionTemplate transactionTemplate;

  @BeforeEach
  void prepararCenarioEmpresa() {
    LimpezaDadosTestSupport.limparDadosNegocio(
        empresaRepository, usuarioRepository, entityManager, transactionTemplate);
    administradorPlataformaRepository.deleteAll();
    PlatformAdminTestSupport.salvarAdministrador(
        administradorPlataformaRepository, passwordEncoder, NOME, EMAIL, SENHA);
  }

  protected String obterTokenPlatformAdmin() throws Exception {
    return PlatformAdminTestSupport.obterToken(mockMvc, EMAIL, SENHA);
  }

  protected UUID criarEmpresaPadrao() throws Exception {
    return criarEmpresaComAdmin().empresaId();
  }

  protected DadosEmpresaCriada criarEmpresaComAdmin() throws Exception {
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

  protected record DadosEmpresaCriada(UUID empresaId, String emailAdmin, String senhaAdmin) {}
}
