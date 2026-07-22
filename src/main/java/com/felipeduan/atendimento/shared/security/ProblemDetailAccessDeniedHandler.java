package com.felipeduan.atendimento.shared.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

  private final AccessDeniedHandler bearerHandler = new BearerTokenAccessDeniedHandler();
  private final JsonMapper jsonMapper;

  public ProblemDetailAccessDeniedHandler(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException excecao)
      throws IOException, ServletException {

    bearerHandler.handle(request, response, excecao);

    ProblemDetail problema =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso.");
    problema.setTitle("Acesso negado");
    problema.setInstance(URI.create(request.getRequestURI()));

    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    jsonMapper.writeValue(response.getOutputStream(), problema);
  }
}
