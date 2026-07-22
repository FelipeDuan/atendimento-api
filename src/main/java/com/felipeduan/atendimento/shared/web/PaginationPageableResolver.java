package com.felipeduan.atendimento.shared.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

final class PaginationPageableResolver implements HandlerMethodArgumentResolver {

  private final int maxPageSize;

  PaginationPageableResolver(int maxPageSize) {
    this.maxPageSize = maxPageSize;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Pageable.class.equals(parameter.getParameterType())
        && parameter.hasParameterAnnotation(Pagination.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory)
      throws Exception {

    Pagination pagination = parameter.getParameterAnnotation(Pagination.class);
    if (pagination == null) {
      throw new IllegalStateException("@Pagination ausente após supportsParameter");
    }

    PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
    resolver.setMaxPageSize(maxPageSize);
    resolver.setFallbackPageable(
        PageRequest.of(0, pagination.size(), Sort.by(pagination.direction(), pagination.sort())));

    Pageable pageable =
        resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    exigirOrdenacaoConhecida(pageable.getSort(), pagination.sort());
    return pageable;
  }

  private void exigirOrdenacaoConhecida(Sort sort, String[] permitidos) {
    Set<String> aceitos = new HashSet<>(Arrays.asList(permitidos));

    for (Sort.Order ordem : sort) {
      if (!aceitos.contains(ordem.getProperty())) {
        throw new OrdenacaoInvalidaException(ordem.getProperty(), Arrays.asList(permitidos));
      }
    }
  }
}
