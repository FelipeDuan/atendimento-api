package com.felipeduan.atendimento.modules.empresas;

import com.felipeduan.atendimento.modules.empresas.dto.CriarEmpresaRequest;
import com.felipeduan.atendimento.modules.empresas.dto.EmpresaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
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
}
