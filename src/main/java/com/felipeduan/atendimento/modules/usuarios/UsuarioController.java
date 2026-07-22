package com.felipeduan.atendimento.modules.usuarios;

import com.felipeduan.atendimento.modules.usuarios.dto.AtualizarUsuarioRequest;
import com.felipeduan.atendimento.modules.usuarios.dto.CriarUsuarioRequest;
import com.felipeduan.atendimento.modules.usuarios.dto.UsuarioResponse;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import com.felipeduan.atendimento.shared.web.Pagination;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários")
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioService usuarioService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse cadastrar(@Valid @RequestBody CriarUsuarioRequest request) {
    return usuarioService.cadastrar(request);
  }

  @GetMapping
  public PageResponse<UsuarioResponse> listar(
      @Pagination(sort = {"dataVinculo", "id.usuarioId"}) Pageable pageable) {
    return usuarioService.listar(pageable);
  }

  @GetMapping("/{id}")
  public UsuarioResponse buscar(@PathVariable UUID id) {
    return usuarioService.buscar(id);
  }

  @PutMapping("/{id}")
  public UsuarioResponse atualizar(
      @PathVariable UUID id, @Valid @RequestBody AtualizarUsuarioRequest request) {
    return usuarioService.atualizar(id, request);
  }
}
