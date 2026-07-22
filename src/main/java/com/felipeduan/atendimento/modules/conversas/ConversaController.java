package com.felipeduan.atendimento.modules.conversas;

import com.felipeduan.atendimento.modules.conversas.dto.AtualizarConversaRequest;
import com.felipeduan.atendimento.modules.conversas.dto.ConversaResponse;
import com.felipeduan.atendimento.modules.conversas.enums.StatusConversa;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import com.felipeduan.atendimento.shared.web.Pagination;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversas")
@Tag(name = "Conversas")
@RequiredArgsConstructor
public class ConversaController {

  private final ConversaService conversaService;

  @GetMapping
  public PageResponse<ConversaResponse> listar(
      @RequestParam(required = false) StatusConversa status,
      @Pagination(sort = {"ultimaInteracao", "id"}) Pageable pageable) {
    return conversaService.listar(status, pageable);
  }

  @GetMapping("/{id}")
  public ConversaResponse buscar(@PathVariable UUID id) {
    return conversaService.buscar(id);
  }

  @PatchMapping("/{id}")
  public ConversaResponse atualizar(
      @PathVariable UUID id, @Valid @RequestBody AtualizarConversaRequest request) {

    return switch (request.acao()) {
      case ENCERRAR -> conversaService.encerrar(id);
      case REABRIR -> conversaService.reabrir(id);
    };
  }
}
