package com.felipeduan.atendimento.modules.platformadmin;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.felipeduan.atendimento.modules.platformadmin.dto.LoginPlataformaRequest;
import com.felipeduan.atendimento.modules.platformadmin.dto.LoginPlataformaResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth/plataforma")
public class PlatformAdminAuthController {

    private final PlatformAdminAuthService authService;

    public PlatformAdminAuthController(PlatformAdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    public LoginPlataformaResponse login(@Valid @RequestBody LoginPlataformaRequest request) {
        return authService.autenticar(request);
    }
}
