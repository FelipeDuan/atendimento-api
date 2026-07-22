package com.felipeduan.atendimento.modules.contatos;

import static com.felipeduan.atendimento.support.ContatoHttpSupport.corpoAtualizarContato;
import static com.felipeduan.atendimento.support.ContatoHttpSupport.corpoCriarContato;
import static com.felipeduan.atendimento.support.ContatoHttpSupport.deleteContato;
import static com.felipeduan.atendimento.support.ContatoHttpSupport.getContato;
import static com.felipeduan.atendimento.support.ContatoHttpSupport.getContatos;
import static com.felipeduan.atendimento.support.ContatoHttpSupport.postContato;
import static com.felipeduan.atendimento.support.ContatoHttpSupport.putContato;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipeduan.atendimento.modules.conversas.AbstractConversaIntegrationTest;
import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContatoCrudIntegrationTest extends AbstractConversaIntegrationTest {

  @Test
  void deveExecutarCrudCompletoComSoftDelete() throws Exception {
    String json =
        postContato(
                mockMvc,
                cenario.tokenAdmin(),
                corpoCriarContato("Ana Contato", "5586999911111", "ana@exemplo.com", "VIP"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nome").value("Ana Contato"))
            .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
            .andExpect(jsonPath("$.observacoes").value("VIP"))
            .andExpect(jsonPath("$.status").value("ATIVO"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String contatoId = JsonPath.read(json, "$.id");

    getContato(mockMvc, cenario.tokenAdmin(), contatoId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numeroWhatsapp").value("5586999911111"));

    putContato(
            mockMvc,
            cenario.tokenAdmin(),
            contatoId,
            corpoAtualizarContato("Ana Atualizada", "ana2@exemplo.com", "Atualizado"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Ana Atualizada"))
        .andExpect(jsonPath("$.email").value("ana2@exemplo.com"));

    deleteContato(mockMvc, cenario.tokenAdmin(), contatoId).andExpect(status().isNoContent());

    getContato(mockMvc, cenario.tokenAdmin(), contatoId).andExpect(status().isNotFound());
  }

  @Test
  void deveRetornar409_quandoNumeroJaCadastradoNaEmpresa() throws Exception {
    postContato(mockMvc, cenario.tokenAdmin(), corpoCriarContato("Primeiro", "5586999922222"))
        .andExpect(status().isCreated());

    postContato(mockMvc, cenario.tokenAdmin(), corpoCriarContato("Segundo", "5586999922222"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Número já cadastrado"));
  }

  @Test
  void naoDeveListarContatoExcluido() throws Exception {
    String json =
        postContato(mockMvc, cenario.tokenAdmin(), corpoCriarContato("Temporário", "5586999933333"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String contatoId = JsonPath.read(json, "$.id");

    deleteContato(mockMvc, cenario.tokenAdmin(), contatoId).andExpect(status().isNoContent());

    String listagem =
        getContatos(mockMvc, cenario.tokenAdmin())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(listagem).doesNotContain(contatoId);
  }

  @Test
  void deveReativarContato_quandoPostRecebeNumeroExcluido() throws Exception {
    String json =
        postContato(
                mockMvc,
                cenario.tokenAdmin(),
                corpoCriarContato("Excluído", "5586999955555", "old@ex.com", "antigo"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String contatoId = JsonPath.read(json, "$.id");

    deleteContato(mockMvc, cenario.tokenAdmin(), contatoId).andExpect(status().isNoContent());
    getContato(mockMvc, cenario.tokenAdmin(), contatoId).andExpect(status().isNotFound());

    postContato(
            mockMvc,
            cenario.tokenAdmin(),
            corpoCriarContato("Reativado HTTP", "5586999955555", "novo@ex.com", "novo"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(contatoId))
        .andExpect(jsonPath("$.nome").value("Reativado HTTP"))
        .andExpect(jsonPath("$.email").value("novo@ex.com"))
        .andExpect(jsonPath("$.observacoes").value("novo"))
        .andExpect(jsonPath("$.status").value("ATIVO"));
  }

  @Test
  void deveReativarContato_quandoLocalizarOuCriarRecebeNumeroExcluido() throws Exception {
    String json =
        postContato(mockMvc, cenario.tokenAdmin(), corpoCriarContato("Reativável", "5586999944444"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String contatoId = JsonPath.read(json, "$.id");

    deleteContato(mockMvc, cenario.tokenAdmin(), contatoId).andExpect(status().isNoContent());

    UUID reativado =
        TenantContext.withTenantId(
            cenario.empresaId(),
            () -> contatoService.localizarOuCriar("5586999944444", "Reativado"));

    assertThat(reativado.toString()).isEqualTo(contatoId);

    getContato(mockMvc, cenario.tokenAdmin(), contatoId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ATIVO"));
  }
}
