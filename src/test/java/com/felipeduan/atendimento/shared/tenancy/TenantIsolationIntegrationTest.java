package com.felipeduan.atendimento.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.felipeduan.atendimento.AbstractIntegrationTest;
import com.felipeduan.atendimento.modules.contatos.Contato;
import com.felipeduan.atendimento.modules.contatos.ContatoService;
import com.felipeduan.atendimento.modules.empresas.EmpresaRepository;
import com.felipeduan.atendimento.modules.usuarios.UsuarioRepository;
import com.felipeduan.atendimento.support.DadosTesteEmpresa;
import com.felipeduan.atendimento.support.LimpezaDadosTestSupport;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

public class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

  @Autowired EmpresaRepository empresaRepository;
  @Autowired ContatoService contatoService;
  @Autowired UsuarioRepository usuarioRepository;
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

    criarContato(empresaA, "Felipe Duan", "5586999990001");
    criarContato(empresaB, "Luís Eduardo", "5586999990002");
  }

  @Test
  void tenantA_VisualizaApenasContatosDaEmpresaA() {
    var contatos = TenantContext.withTenantId(empresaA, contatoService::listarTodos);
    var primeiroContato = contatos.getFirst();

    assertThat(contatos).hasSize(1);
    assertThat(primeiroContato.getNome()).isEqualTo("Felipe Duan");
    assertThat(primeiroContato.getNumeroWhatsapp()).isEqualTo("5586999990001");
  }

  @Test
  void tenantB_VisualizaApenasContatosDaEmpresaB() {
    var contatos = TenantContext.withTenantId(empresaB, contatoService::listarTodos);
    var primeiroContato = contatos.getFirst();

    assertThat(contatos).hasSize(1);
    assertThat(primeiroContato.getNome()).isEqualTo("Luís Eduardo");
    assertThat(primeiroContato.getNumeroWhatsapp()).isEqualTo("5586999990002");
  }

  @Test
  void semTenant_NaoVisualizaContatos() {
    var contatos = contatoService.listarTodos();
    assertThat(contatos).hasSize(0);
  }

  private void criarContato(UUID empresaId, String nome, String whatsapp) {
    TenantContext.withTenantId(
        empresaId, () -> contatoService.salvar(new Contato(empresaId, nome, whatsapp)));
  }
}
