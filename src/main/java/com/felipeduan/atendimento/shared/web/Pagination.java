package com.felipeduan.atendimento.shared.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.data.domain.Sort;

@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pagination {

  int size() default 20;

  String[] sort() default {"dataCriacao", "id"};

  Sort.Direction direction() default Sort.Direction.DESC;
}
