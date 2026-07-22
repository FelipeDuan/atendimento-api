package com.felipeduan.atendimento.modules.mensagens;

import com.felipeduan.atendimento.modules.mensagens.dto.EnviarMensagemRequest;
import com.felipeduan.atendimento.modules.mensagens.dto.MensagemResponse;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import com.felipeduan.atendimento.shared.web.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mensagens")
@Tag(name = "Mensagens")
@RequiredArgsConstructor
public class MensagemController {

  private final MensagemService mensagemService;

  @GetMapping
  @Operation(operationId = "listarMensagens")
  public PageResponse<MensagemResponse> listar(
      @RequestParam UUID conversaId,
      @Pagination(
              sort = {"dataHora", "id"},
              direction = Sort.Direction.ASC)
          Pageable pageable) {
    return mensagemService.listar(conversaId, pageable);
  }

  @GetMapping("/{id}")
  @Operation(operationId = "buscarMensagem")
  public MensagemResponse buscar(@PathVariable UUID id) {
    return mensagemService.buscar(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(operationId = "enviarMensagem")
  public MensagemResponse enviar(@Valid @RequestBody EnviarMensagemRequest request) {
    return mensagemService.enviar(request);
  }
}
