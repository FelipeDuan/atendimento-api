package com.felipeduan.atendimento.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Atendimento API")
                .description(
                    "API REST multi-tenant para atendimento via WhatsApp. "
                        + "Contrato, autenticação e fluxos de verificação estão no README.")
                .version("v1"))
        .servers(List.of(new Server().url("/").description("Instância local")))
        .tags(
            List.of(
                new Tag().name("Auth"),
                new Tag().name("Empresas"),
                new Tag().name("Usuários"),
                new Tag().name("Contatos"),
                new Tag().name("Conversas"),
                new Tag().name("Mensagens"),
                new Tag().name("Webhook")))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearer-key",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearer-key"));
  }
}
