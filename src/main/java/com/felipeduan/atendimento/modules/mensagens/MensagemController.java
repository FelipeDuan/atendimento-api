package com.felipeduan.atendimento.modules.mensagens;

import com.felipeduan.atendimento.modules.conversas.dto.MensagemResponse;
import com.felipeduan.atendimento.modules.mensagens.dto.EnviarMensagemRequest;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import com.felipeduan.atendimento.shared.web.Pagination;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversas/{conversaId}/mensagens")
@RequiredArgsConstructor
public class MensagemController {

  private final MensagemService mensagemService;

  @GetMapping
  public PageResponse<MensagemResponse> listar(
      @PathVariable UUID conversaId,
      @Pagination(
              sort = {"dataHora", "id"},
              direction = Sort.Direction.ASC)
          Pageable pageable) {
    return mensagemService.listar(conversaId, pageable);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MensagemResponse enviar(
      @PathVariable UUID conversaId, @Valid @RequestBody EnviarMensagemRequest request) {
    return mensagemService.enviar(conversaId, request);
  }
}
