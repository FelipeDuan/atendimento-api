package com.felipeduan.atendimento.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.felipeduan.atendimento.AbstractIntegrationTest;
import com.felipeduan.atendimento.modules.empresas.EmpresaRepository;
import com.felipeduan.atendimento.modules.usuarios.Usuario;
import com.felipeduan.atendimento.modules.usuarios.UsuarioRepository;
import com.felipeduan.atendimento.modules.vinculos.UsuarioEmpresaRepository;
import com.felipeduan.atendimento.modules.vinculos.VinculoService;
import com.felipeduan.atendimento.support.DadosTesteEmpresa;
import com.felipeduan.atendimento.support.LimpezaDadosTestSupport;
import com.felipeduan.atendimento.support.RlsTestSupport;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

class UsuarioEmpresaTenantIsolationIntegrationTest extends AbstractIntegrationTest {

  @Autowired EmpresaRepository empresaRepository;
  @Autowired UsuarioRepository usuarioRepository;
  @Autowired UsuarioEmpresaRepository usuarioEmpresaRepository;
  @Autowired VinculoService vinculoService;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired EntityManager entityManager;
  @Autowired TransactionTemplate transactionTemplate;

  UUID empresaA;
  UUID empresaB;

  @BeforeEach
  void prepararDados() {
    LimpezaDadosTestSupport.limparDadosNegocio(
        empresaRepository, usuarioRepository, entityManager, transactionTemplate);

    empresaA = DadosTesteEmpresa.criar(empresaRepository, "Empresa A");
    empresaB = DadosTesteEmpresa.criar(empresaRepository, "Empresa B");

    String emailUnico = "compartilhado-" + UUID.randomUUID() + "@vinculo.test";

    Usuario usuario =
        usuarioRepository.save(
            Usuario.criarComSenhaTemporaria(
                "Usuario Compartilhado",
                emailUnico,
                passwordEncoder.encode("SenhaTemp123!"),
                empresaA));

    UUID usuarioId = usuario.getId();
    TenantContext.withTenantId(
        empresaA, () -> vinculoService.vincularComoAdministrador(usuarioId, empresaA));
    TenantContext.withTenantId(
        empresaB, () -> vinculoService.vincularComoAdministrador(usuarioId, empresaB));
  }

  @Test
  void tenantA_veApenasVinculoDaEmpresaA() {
    assertThat(contarVinculos(empresaA)).isEqualTo(1);
  }

  @Test
  void tenantB_veApenasVinculoDaEmpresaB() {
    assertThat(contarVinculos(empresaB)).isEqualTo(1);
  }

  @Test
  void semTenant_naoVeVinculos() {
    assertThat(usuarioEmpresaRepository.count()).isZero();
  }

  private long contarVinculos(UUID empresaId) {
    return RlsTestSupport.contarVinculosDaEmpresa(
        empresaId, usuarioEmpresaRepository, entityManager, transactionTemplate);
  }
}
