package com.felipeduan.atendimento.modules.platformadmin;

import com.felipeduan.atendimento.modules.platformadmin.dto.LoginPlataformaRequest;
import com.felipeduan.atendimento.modules.platformadmin.dto.LoginPlataformaResponse;
import com.felipeduan.atendimento.modules.platformadmin.exception.CredenciaisInvalidasException;
import com.felipeduan.atendimento.shared.security.JwtService;
import com.felipeduan.atendimento.shared.security.Roles;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformAdminAuthService {

  private final AdministradorPlataformaRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public PlatformAdminAuthService(
      AdministradorPlataformaRepository repository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional(readOnly = true)
  public LoginPlataformaResponse autenticar(LoginPlataformaRequest request) {
    var administrador =
        repository
            .findByEmail(request.email())
            .filter(admin -> passwordEncoder.matches(request.senha(), admin.getSenhaHash()))
            .orElseThrow(CredenciaisInvalidasException::new);

    String accessToken =
        jwtService.emitirToken(
            administrador.getId().toString(), List.of(Roles.PLATFORM_ADMIN), null);

    return new LoginPlataformaResponse(accessToken);
  }
}
