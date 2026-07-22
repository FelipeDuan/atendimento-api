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

  private static final String DESCRICAO =
      """
      API REST multi-tenant para atendimento via WhatsApp.

      ## Como navegar neste console
      1. **Auth** — `POST /auth/plataforma/login` com o Platform Admin do `.env`
      2. **Empresas** — `POST /empresas` (provisiona tenant e administrador inicial)
      3. **Auth** — `POST /auth/login` com esse admin; se `exigeTrocarSenha`,
         use `POST /auth/trocar-senha` e autentique novamente
      4. **Authorize** — cole apenas o `accessToken` (o Scalar envia como Bearer)
      5. Contatos → Conversas → Mensagens — fluxo operacional do tenant
      6. **Webhook** — `GET /webhooks/messages` (challenge) e `POST` com
         `X-Hub-Signature-256` (HMAC-SHA256 do body bruto, chave `META_APP_SECRET`)

      O tenant é o claim `tenant_id` do JWT. Não envie header de tenant.
      """;

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info().title("Atendimento API").description(DESCRICAO).version("v1"))
        .servers(List.of(new Server().url("/").description("Ambiente local (profile dev)")))
        .tags(
            List.of(
                new Tag()
                    .name("Auth")
                    .description("Autenticação, troca de senha e troca de tenant"),
                new Tag()
                    .name("Empresas")
                    .description("Gestão de tenants; POST e DELETE exigem PLATFORM_ADMIN"),
                new Tag()
                    .name("Usuários")
                    .description("Vínculos do tenant (perfil e status por empresa)"),
                new Tag()
                    .name("Contatos")
                    .description("CRUD de contatos com soft delete por status"),
                new Tag()
                    .name("Conversas")
                    .description("Abertura, listagem, encerramento e reabertura"),
                new Tag()
                    .name("Mensagens")
                    .description("Registro de saída e consulta paginada por conversa"),
                new Tag()
                    .name("Webhook")
                    .description("Meta Cloud API — verificação (GET) e ingestão assinada (POST)")))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearer-key",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "JWT obtido no login. Informe só o token; o Scalar prefixa Bearer.")))
        .addSecurityItem(new SecurityRequirement().addList("bearer-key"));
  }
}
