package com.felipeduan.atendimento.shared.exception;

import com.felipeduan.atendimento.modules.auth.exceptions.LoginCredenciaisInvalidasException;
import com.felipeduan.atendimento.modules.auth.exceptions.SemAcessoEmpresaException;
import com.felipeduan.atendimento.modules.auth.exceptions.SemVinculoAtivoException;
import com.felipeduan.atendimento.modules.contatos.exception.ContatoNaoEncontradoException;
import com.felipeduan.atendimento.modules.conversas.exception.ConversaEncerradaException;
import com.felipeduan.atendimento.modules.conversas.exception.ConversaNaoEncontradaException;
import com.felipeduan.atendimento.modules.conversas.exception.EstadoConversaInvalidoException;
import com.felipeduan.atendimento.modules.empresas.exception.CnpjJaCadastradoException;
import com.felipeduan.atendimento.modules.empresas.exception.EmpresaNaoEncontradaException;
import com.felipeduan.atendimento.modules.mensagens.exception.MensagemNaoEncontradaException;
import com.felipeduan.atendimento.modules.platformadmin.exception.CredenciaisInvalidasException;
import com.felipeduan.atendimento.modules.usuarios.exception.EmailExistenteSenhaInvalidaException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    List<CampoInvalido> erros = new ArrayList<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      erros.add(new CampoInvalido(fieldError.getField(), fieldError.getDefaultMessage()));
    }

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos.");
    problem.setTitle("Dados inválidos");
    problem.setProperty("errors", erros);

    return handleExceptionInternal(ex, problem, headers, status, request);
  }

  @ExceptionHandler({CredenciaisInvalidasException.class, LoginCredenciaisInvalidasException.class})
  public ProblemDetail handleCredenciaisInvalidas(DomainException ex) {
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

  @ExceptionHandler(EmpresaNaoEncontradaException.class)
  public ProblemDetail handleEmpresaNaoEncontrada(EmpresaNaoEncontradaException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Empresa não encontrada");
    return problem;
  }

  @ExceptionHandler(SemVinculoAtivoException.class)
  public ProblemDetail handleSemVinculoAtivo(SemVinculoAtivoException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problem.setTitle("Sem vínculo ativo");
    return problem;
  }

  @ExceptionHandler(SemAcessoEmpresaException.class)
  public ProblemDetail handleSemAcessoEmpresa(SemAcessoEmpresaException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problem.setTitle("Acesso negado");
    return problem;
  }

  @ExceptionHandler(ConversaEncerradaException.class)
  public ProblemDetail handleConversaEncerrada(ConversaEncerradaException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Conversa encerrada");
    return problem;
  }

  @ExceptionHandler(EstadoConversaInvalidoException.class)
  public ProblemDetail handleEstadoConversaInvalido(EstadoConversaInvalidoException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Estado inválido");
    return problem;
  }

  @ExceptionHandler(ConversaNaoEncontradaException.class)
  public ProblemDetail handleConversaNaoEncontrada(ConversaNaoEncontradaException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Conversa não encontrada");
    return problem;
  }

  @ExceptionHandler(MensagemNaoEncontradaException.class)
  public ProblemDetail handleMensagemNaoEncontrada(MensagemNaoEncontradaException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Mensagem não encontrada");
    return problem;
  }

  @ExceptionHandler(ContatoNaoEncontradoException.class)
  public ProblemDetail handleContatoNaoEncontrado(ContatoNaoEncontradoException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Contato não encontrado");
    return problem;
  }
}
