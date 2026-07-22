package com.felipeduan.atendimento.shared.web;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class PaginationWebConfig implements WebMvcConfigurer {

  private final int maxPageSize;

  PaginationWebConfig(@Value("${spring.data.web.pageable.max-page-size:2000}") int maxPageSize) {
    this.maxPageSize = maxPageSize;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(0, new PaginationPageableResolver(maxPageSize));
  }
}
