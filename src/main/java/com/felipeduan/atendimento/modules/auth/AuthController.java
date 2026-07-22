package com.felipeduan.atendimento.modules.auth;

import com.felipeduan.atendimento.modules.auth.dto.LoginRequest;
import com.felipeduan.atendimento.modules.auth.dto.LoginResponse;
import com.felipeduan.atendimento.modules.auth.dto.SwitchTenantRequest;
import com.felipeduan.atendimento.modules.auth.dto.SwitchTenantResponse;
import com.felipeduan.atendimento.modules.auth.dto.TrocarSenhaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService service;

  @PostMapping("/login")
  @SecurityRequirements
  @Operation(operationId = "login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return service.login(request);
  }

  @PostMapping("/trocar-senha")
  @Operation(operationId = "trocarSenha")
  public LoginResponse trocarSenha(@Valid @RequestBody TrocarSenhaRequest request) {
    return service.trocarSenha(request);
  }

  @PostMapping("/switch-tenant")
  @Operation(
      operationId = "switchTenant",
      summary = "Reemite o token para outra empresa vinculada, sem pedir senha")
  public SwitchTenantResponse switchTenant(@Valid @RequestBody SwitchTenantRequest request) {
    return service.trocarTenant(request);
  }
}
