package com.felipeduan.atendimento;

import com.felipeduan.atendimento.shared.config.JwtProperties;
import com.felipeduan.atendimento.shared.config.MetaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({JwtProperties.class, MetaProperties.class})
@SpringBootApplication
public class AtendimentoApplication {

  public static void main(String[] args) {
    SpringApplication.run(AtendimentoApplication.class, args);
  }
}
