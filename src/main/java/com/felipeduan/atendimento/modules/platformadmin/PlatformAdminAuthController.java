package com.felipeduan.atendimento.modules.platformadmin;

import com.felipeduan.atendimento.modules.platformadmin.dto.LoginPlataformaRequest;
import com.felipeduan.atendimento.modules.platformadmin.dto.LoginPlataformaResponse;
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
@RequestMapping("/auth/plataforma")
@Tag(name = "Auth")
@RequiredArgsConstructor
public class PlatformAdminAuthController {

  private final PlatformAdminAuthService authService;

  @PostMapping("/login")
  @SecurityRequirements
  @Operation(operationId = "loginPlataforma")
  public LoginPlataformaResponse login(@Valid @RequestBody LoginPlataformaRequest request) {
    return authService.autenticar(request);
  }
}
