package com.felipeduan.atendimento.modules.empresas;

import com.felipeduan.atendimento.modules.empresas.dto.AtualizarEmpresaRequest;
import com.felipeduan.atendimento.modules.empresas.dto.CriarEmpresaRequest;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResponse;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResumoResponse;
import com.felipeduan.atendimento.shared.dto.PageResponse;
import com.felipeduan.atendimento.shared.web.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
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
@Tag(name = "Empresas")
@RequiredArgsConstructor
public class EmpresaController {

  private final EmpresaService empresaService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(operationId = "criarEmpresa")
  public EmpresaResponse criar(@Valid @RequestBody CriarEmpresaRequest request) {
    return empresaService.criar(request);
  }

  @GetMapping
  @Operation(operationId = "listarEmpresas")
  public PageResponse<EmpresaResumoResponse> listar(
      @ParameterObject @Pagination(sort = {"dataCriacao", "id", "nome"}) Pageable pageable) {
    return empresaService.listarAtivas(pageable);
  }

  @GetMapping("/inativas")
  @Operation(operationId = "listarEmpresasInativas")
  public PageResponse<EmpresaResumoResponse> listarInativas(
      @ParameterObject @Pagination(sort = {"dataCriacao", "id", "nome"}) Pageable pageable) {
    return empresaService.listarInativas(pageable);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasPermission(#id, 'Empresa', 'read')")
  @Operation(operationId = "buscarEmpresa")
  public EmpresaResponse buscar(@PathVariable UUID id) {
    return empresaService.buscar(id);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasPermission(#id, 'Empresa', 'write')")
  @Operation(operationId = "atualizarEmpresa")
  public EmpresaResponse atualizar(
      @PathVariable UUID id, @Valid @RequestBody AtualizarEmpresaRequest request) {
    return empresaService.atualizar(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "inativarEmpresa")
  public void inativar(@PathVariable UUID id) {
    empresaService.inativar(id);
  }
}
