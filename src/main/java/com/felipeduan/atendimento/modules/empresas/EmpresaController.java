package com.felipeduan.atendimento.modules.empresas;

import com.felipeduan.atendimento.modules.empresas.dto.AtualizarEmpresaRequest;
import com.felipeduan.atendimento.modules.empresas.dto.CriarEmpresaRequest;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResponse;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResumoResponse;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/empresas")
@RequiredArgsConstructor
public class EmpresaController {

  private final EmpresaService empresaService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EmpresaResponse criar(@Valid @RequestBody CriarEmpresaRequest request) {
    return empresaService.criar(request);
  }

  @GetMapping
  public PageResponse<EmpresaResumoResponse> listar(
      @PageableDefault(
              size = 20,
              sort = {"dataCriacao", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    return empresaService.listarAtivas(pageable);
  }

  @GetMapping("/inativas")
  public PageResponse<EmpresaResumoResponse> listarInativas(
      @PageableDefault(
              size = 20,
              sort = {"dataCriacao", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    return empresaService.listarInativas(pageable);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasPermission(#id, 'Empresa', 'read')")
  public EmpresaResponse buscar(@PathVariable UUID id) {
    return empresaService.buscar(id);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasPermission(#id, 'Empresa', 'write')")
  public EmpresaResponse atualizar(
      @PathVariable UUID id, @Valid @RequestBody AtualizarEmpresaRequest request) {
    return empresaService.atualizar(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void inativar(@PathVariable UUID id) {
    empresaService.inativar(id);
  }
}
