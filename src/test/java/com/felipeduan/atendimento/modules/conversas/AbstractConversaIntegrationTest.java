package com.felipeduan.atendimento.modules.conversas;

import static com.felipeduan.atendimento.support.AuthHttpSupport.obterToken;
import static com.felipeduan.atendimento.support.AuthIntegrationTestSupport.liberarSenhaDefinitiva;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.cnpjUnico;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.corpoCriarEmpresa;
import static com.felipeduan.atendimento.support.EmpresaHttpSupport.postCriarEmpresa;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.EMAIL;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.NOME;
import static com.felipeduan.atendimento.support.PlatformAdminTestSupport.SENHA;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.AbstractIntegrationTest;
import com.felipeduan.atendimento.modules.contatos.ContatoService;
import com.felipeduan.atendimento.modules.empresas.EmpresaRepository;
import com.felipeduan.atendimento.modules.platformadmin.AdministradorPlataformaRepository;
import com.felipeduan.atendimento.modules.usuarios.UsuarioRepository;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import com.felipeduan.atendimento.support.LimpezaDadosTestSupport;
import com.felipeduan.atendimento.support.PlatformAdminTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfigureMockMvc
public abstract class AbstractConversaIntegrationTest extends AbstractIntegrationTest {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected AdministradorPlataformaRepository administradorPlataformaRepository;
  @Autowired protected EmpresaRepository empresaRepository;
  @Autowired protected UsuarioRepository usuarioRepository;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected EntityManager entityManager;
  @Autowired protected TransactionTemplate transactionTemplate;
  @Autowired protected ContatoService contatoService;
  @Autowired protected ConversaService conversaService;

  protected CenarioConversa cenario;

  @BeforeEach
  void prepararCenarioConversa() throws Exception {
    LimpezaDadosTestSupport.limparDadosNegocio(
        usuarioRepository, entityManager, transactionTemplate);
    administradorPlataformaRepository.deleteAll();
    PlatformAdminTestSupport.salvarAdministrador(
        administradorPlataformaRepository, passwordEncoder, NOME, EMAIL, SENHA);

    String cnpj = cnpjUnico();
    String emailAdmin = "admin-" + cnpj + "@empresa.local";
    String senhaAdmin = "SenhaTemp123!";
    String tokenPlatform = PlatformAdminTestSupport.obterToken(mockMvc, EMAIL, SENHA);

    String jsonEmpresa =
        postCriarEmpresa(mockMvc, tokenPlatform, corpoCriarEmpresa(cnpj, emailAdmin, senhaAdmin))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID empresaId = UUID.fromString(JsonPath.read(jsonEmpresa, "$.id"));
    liberarSenhaDefinitiva(usuarioRepository, passwordEncoder, emailAdmin, senhaAdmin);
    String tokenAdmin = obterToken(mockMvc, emailAdmin, senhaAdmin);

    UUID contatoId =
        TenantContext.withTenantId(
            empresaId, () -> contatoService.localizarOuCriar("5586999900001", "Cliente Teste"));

    UUID conversaId =
        TenantContext.withTenantId(
            empresaId, () -> conversaService.garantirConversaAberta(contatoId));

    cenario =
        new CenarioConversa(empresaId, contatoId, conversaId, emailAdmin, senhaAdmin, tokenAdmin);
  }

  protected record CenarioConversa(
      UUID empresaId,
      UUID contatoId,
      UUID conversaId,
      String emailAdmin,
      String senhaAdmin,
      String tokenAdmin) {}
}
