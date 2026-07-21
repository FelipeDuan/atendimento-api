package com.felipeduan.atendimento.shared.exception;

import com.felipeduan.atendimento.modules.empresas.exception.CnpjJaCadastradoException;
import com.felipeduan.atendimento.modules.platformadmin.exception.CredenciaisInvalidasException;
import com.felipeduan.atendimento.modules.usuarios.exception.EmailExistenteSenhaInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(CredenciaisInvalidasException.class)
  public ProblemDetail handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    problem.setTitle("Credenciais Inválidas");
    return problem;
  }

  @ExceptionHandler(CnpjJaCadastradoException.class)
  public ProblemDetail handleCnpjJaCadastrado(CnpjJaCadastradoException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("CNPJ já cadastrado");
    return problem;
  }

  @ExceptionHandler(EmailExistenteSenhaInvalidaException.class)
  public ProblemDetail handleEmailExistente(EmailExistenteSenhaInvalidaException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("E-mail já cadastrado");
    return problem;
  }
}
