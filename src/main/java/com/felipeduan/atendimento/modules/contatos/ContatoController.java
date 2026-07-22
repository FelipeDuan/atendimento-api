package com.felipeduan.atendimento.modules.contatos;

import com.felipeduan.atendimento.modules.contatos.dto.AtualizarContatoRequest;
import com.felipeduan.atendimento.modules.contatos.dto.ContatoResponse;
import com.felipeduan.atendimento.modules.contatos.dto.CriarContatoRequest;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import com.felipeduan.atendimento.shared.web.Pagination;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contatos")
@Tag(name = "Contatos")
@RequiredArgsConstructor
public class ContatoController {

  private final ContatoService contatoService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ContatoResponse criar(@Valid @RequestBody CriarContatoRequest request) {
    return contatoService.criar(request);
  }

  @GetMapping
  public PageResponse<ContatoResponse> listar(
      @Pagination(
              sort = {"nome", "id"},
              direction = Sort.Direction.ASC)
          Pageable pageable) {
    return contatoService.listar(pageable);
  }

  @GetMapping("/{id}")
  public ContatoResponse buscar(@PathVariable UUID id) {
    return contatoService.buscar(id);
  }

  @PutMapping("/{id}")
  public ContatoResponse atualizar(
      @PathVariable UUID id, @Valid @RequestBody AtualizarContatoRequest request) {
    return contatoService.atualizar(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void inativar(@PathVariable UUID id) {
    contatoService.inativar(id);
  }
}
