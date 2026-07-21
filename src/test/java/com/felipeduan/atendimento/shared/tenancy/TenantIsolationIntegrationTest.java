package com.felipeduan.atendimento.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.felipeduan.atendimento.AbstractIntegrationTest;
import com.felipeduan.atendimento.modules.contatos.Contato;
import com.felipeduan.atendimento.modules.contatos.ContatoService;
import com.felipeduan.atendimento.modules.empresas.Empresa;
import com.felipeduan.atendimento.modules.empresas.EmpresaService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

  @Autowired EmpresaService empresaService;
  @Autowired ContatoService contatoService;

  UUID empresaA;
  UUID empresaB;

  @BeforeEach
  void prepararDados() {
    empresaA = criarEmpresa("Empresa A");
    empresaB = criarEmpresa("Empresa B");
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

  private UUID criarEmpresa(String nome) {
    String cnpj = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
    String email = cnpj + "email@empresa.com";
    return empresaService.salvar(new Empresa(nome, cnpj, email)).getId();
  }

  private void criarContato(UUID empresaId, String nome, String whatsapp) {
    TenantContext.withTenantId(
        empresaId, () -> contatoService.salvar(new Contato(empresaId, nome, whatsapp)));
  }
}
