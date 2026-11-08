# Plano de implementação — Cache (Redis) + Rate Limiting

**Data:** 2026-07-23 · **Branch sugerida:** `feat/cache-rate-limit` (a partir da atual)
**Autor:** revisão Claude · **Executor:** Cursor
**Regra de ouro:** cada seção é um commit; `make test-fresh` verde por commit; não seguir para o próximo com o gate vermelho.

---

## 0. Fatos verificados nesta sessão (não são suposição)

Levantados contra os jars resolvidos e o código atual — **confie neles**:

| Fato | Como verifiquei | Consequência de design |
|---|---|---|
| Boot 4.1 usa **Jackson 3** (`tools.jackson` 3.1.4); Jackson 2 (2.21.4) está transitivo | `gradlew dependencies` | Serializer de cache = `GenericJacksonJsonRedisSerializer` (Jackson 3), **não** `GenericJackson2JsonRedisSerializer` |
| Existe um `JsonMapper` (Jackson 3) gerenciado pelo Spring | injetado hoje em `WebhookService` e `ProblemDetailAccessDeniedHandler` | Reusar esse bean no serializer, não criar outro ObjectMapper |
| `GenericJacksonJsonRedisSerializer(tools.jackson.databind.ObjectMapper)` + `.builder()` existem | `javap` no `spring-data-redis-4.1.0.jar` | Construtor e builder confirmados |
| `RedisCacheManager` / `RedisCacheConfiguration` presentes | `unzip -l` no jar | Config de cache padrão do Spring, sem lib extra |
| `spring-boot-starter-data-redis` 4.1.0 + **Lettuce 7.5.2** já resolvidos | `gradlew dependencies` | Infra Redis já existe; **Lettuce 7 é o ponto de risco do rate limit** (ver §5) |
| `AtendimentoPermissionEvaluator` **não** bate no banco (lê tenant do JWT) | leitura do código | **Não** é candidato a cache |
| Único candidato real a cache: `EmpresaService.buscarIdPorPhoneNumberId` | leitura do código | É chamado pelo `WebhookService` (bean diferente → proxy `@Cacheable` aplica) |
| `buscarIdPorPhoneNumberId` roda **antes** de haver tenant (é o resolvedor de tenant) | fluxo do webhook | Essa chave **não** usa `tenantKeyGenerator` — é global por `phone_number_id` (documentar o porquê) |
| `criar` de empresa não precisa de eviction | não cacheamos negativos (`unless #result==null`) | Só `atualizar`/`inativar` invalidam |

**Dois pontos version-sensitive** que o Cursor **precisa** confirmar no build (têm fallback na §5):
1. Compatibilidade `bucket4j-lettuce` × **Lettuce 7.5** (Bucket4j historicamente mira Lettuce 6).
2. API exata do `Bandwidth` builder na versão do Bucket4j resolvida.

---

## 1. Filosofia (ler antes de codar)

Num sistema multi-tenant com RLS, **cache é um risco de segurança**, não um detalhe de performance. O `CLAUDE.md` é explícito (invariante 3): *todo `@Cacheable` de dado de tenant usa `keyGenerator = "tenantKeyGenerator"` — sem tenant na chave, o cache vira bypass do RLS entre tenants.*

Por isso este plano **não** sai espalhando `@Cacheable`. Cacheia **uma** coisa — a resolução `phone_number_id → empresaId`, que é a hot path do webhook e **não** é dado de tenant (é o que resolve o tenant). E monta a **infraestrutura fail-closed** (`tenantKeyGenerator` que estoura sem tenant) para que qualquer cache futuro de dado de tenant seja seguro por construção.

Qualidade aqui é **cachear pouco e certo**, não muito.

---

## 2. CACHE — commits C1–C3

### C1 — `feat(cache): infraestrutura de cache Redis com chave por tenant fail-closed`

#### 2.1 `build.gradle.kts` — habilitar cache (a dependência Redis já existe)

Na seção `dependencies`, adicionar:

```kotlin
	implementation("org.springframework.boot:spring-boot-starter-cache")
```

> `spring-boot-starter-data-redis` já está no projeto. O `starter-cache` traz a abstração `@Cacheable`/`CacheManager`.

#### 2.2 Novo `shared/cache/TenantKeyGenerator.java`

O mandato do `CLAUDE.md`. **Fail-closed**: se alguém anotar um método de dado de tenant com este gerador e não houver tenant no contexto, estoura — nunca cacheia sob chave sem tenant.

```java
package com.felipeduan.atendimento.shared.cache;

import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

/**
 * Prefixa toda chave de cache com o tenant corrente. Sem isto, o cache de um dado de tenant vira
 * bypass do RLS: o tenant B leria do cache um valor gravado pelo tenant A (invariante 3 do CLAUDE.md).
 *
 * <p>Fail-closed de propósito: se não há tenant no contexto, lança — é erro de programação anotar um
 * dado de tenant com este gerador fora de um escopo de tenant. Caches que NÃO são de tenant (ex.:
 * resolução de {@code phone_number_id}) não usam este gerador.
 */
@Component("tenantKeyGenerator")
public class TenantKeyGenerator implements KeyGenerator {

  @Override
  public Object generate(Object target, Method method, Object... params) {
    UUID tenantId =
        TenantContext.getTenantId()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Cache de dado de tenant sem tenant_id no contexto: "
                            + target.getClass().getSimpleName()
                            + "#"
                            + method.getName()));
    return tenantId + ":" + method.getName() + ":" + Arrays.deepToString(params);
  }
}
```

#### 2.3 Novo `shared/cache/CacheConfig.java`

Nomes de cache como constantes (evita string mágica espalhada). Serializer Jackson 3 (fato verificado). TTL por cache. Null não é cacheado. Prefixo com o nome da app.

```java
package com.felipeduan.atendimento.shared.cache;

import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableCaching
public class CacheConfig {

  /** Resolução phone_number_id → empresaId. NÃO é dado de tenant: é o que resolve o tenant. */
  public static final String EMPRESA_ID_POR_PHONE_NUMBER_ID = "empresaIdPorPhoneNumberId";

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, JsonMapper jsonMapper) {
    SerializationPair<Object> valores =
        SerializationPair.fromSerializer(new GenericJacksonJsonRedisSerializer(jsonMapper));

    RedisCacheConfiguration base =
        RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(valores)
            .disableCachingNullValues()
            .prefixCacheNameWith("atendimento::");

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(base.entryTtl(Duration.ofMinutes(10)))
        .withCacheConfiguration(
            EMPRESA_ID_POR_PHONE_NUMBER_ID, base.entryTtl(Duration.ofMinutes(10)))
        .build();
  }
}
```

> **Verify-point (baixo risco):** `prefixCacheNameWith`, `entryTtl`, `disableCachingNullValues`,
> `serializeValuesWith`, `SerializationPair.fromSerializer` são API estável do Spring Data Redis 3.x–4.x.
> Se algum nome divergir na 4.1, é rename trivial — compile e ajuste. O construtor
> `GenericJacksonJsonRedisSerializer(JsonMapper)` está **confirmado por javap**.

#### 2.4 Teste `shared/cache/TenantKeyGeneratorTest.java` (unitário, sem Spring)

```java
package com.felipeduan.atendimento.shared.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.felipeduan.atendimento.shared.tenancy.TenantContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantKeyGeneratorTest {

  private final TenantKeyGenerator generator = new TenantKeyGenerator();

  @AfterEach
  void limpar() {
    TenantContext.clear();
  }

  @Test
  void deveIncluirTenantNaChave_quandoHaTenant() throws Exception {
    UUID tenant = UUID.randomUUID();
    TenantContext.setTenantId(tenant);
    Object chave = generator.generate(this, metodoQualquer(), "arg1");
    assertThat(chave.toString()).startsWith(tenant.toString());
  }

  @Test
  void deveLancar_quandoSemTenant() throws Exception {
    var metodo = metodoQualquer();
    assertThatThrownBy(() -> generator.generate(this, metodo, "arg1"))
        .isInstanceOf(IllegalStateException.class);
  }

  private java.lang.reflect.Method metodoQualquer() throws NoSuchMethodException {
    return TenantKeyGeneratorTest.class.getDeclaredMethod("metodoQualquer");
  }
}
```

**Gate C1:** `make test-fresh` verde. A app sobe com `CacheManager` Redis (Testcontainers já provê Redis nos testes — confirmar que `@ServiceConnection` do Redis existe na config de teste; se não existir, adicionar, ver nota no fim da §2).

---

### C2 — `feat(empresas): cacheia resolução phone_number_id → empresaId`

#### 2.5 `EmpresaService.buscarIdPorPhoneNumberId` — anotar

```java
import com.felipeduan.atendimento.shared.cache.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
// ...

  // Cache global por phone_number_id (NÃO é dado de tenant — é o que resolve o tenant, roda antes
  // de haver TenantContext). unless: negativos não são cacheados para não mascarar cadastro novo.
  @Cacheable(
      cacheNames = CacheConfig.EMPRESA_ID_POR_PHONE_NUMBER_ID,
      key = "#phoneNumberId",
      unless = "#result == null")
  @Transactional(readOnly = true)
  public Optional<UUID> buscarIdPorPhoneNumberId(String phoneNumberId) {
    return empresaRepository.findByPhoneNumberId(phoneNumberId).map(Empresa::getId);
  }
```

> **Por que `#result == null` e não `!#result.isPresent()`:** para tipos `Optional`, o Spring
> disponibiliza em `condition`/`unless` o **valor desembrulhado** em `#result` (não o wrapper). Empty →
> `#result` é `null` → não cacheia. Confirmado no comportamento do `CacheAspectSupport`.

#### 2.6 `EmpresaService.atualizar` e `inativar` — invalidar

`phone_number_id` pode **mudar** de um valor para outro; `@CacheEvict(key=...)` só conhece o valor
novo, não o antigo. Escritas em empresa são raras e o cache é pequeno → `allEntries = true` é a escolha
correta e provadamente segura.

```java
  @CacheEvict(cacheNames = CacheConfig.EMPRESA_ID_POR_PHONE_NUMBER_ID, allEntries = true)
  @Transactional
  public EmpresaResponse atualizar(UUID id, AtualizarEmpresaRequest request) { /* inalterado */ }

  @CacheEvict(cacheNames = CacheConfig.EMPRESA_ID_POR_PHONE_NUMBER_ID, allEntries = true)
  @Transactional
  public void inativar(UUID id) { /* inalterado */ }
```

> `criar` **não** recebe `@CacheEvict`: como negativos não são cacheados, um `phone_number_id` novo
> nunca esteve no cache. Não invente eviction onde não há stale.

#### 2.7 Teste `modules/empresas/EmpresaCachePhoneNumberIdIntegrationTest.java`

Prova os três comportamentos que importam: **hit** (segunda chamada não vai ao banco), **eviction** ao
mudar o phone, e **ausência de negativo cacheado**.

```java
package com.felipeduan.atendimento.modules.empresas;

import static org.assertj.core.api.Assertions.assertThat;

import com.felipeduan.atendimento.AbstractIntegrationTest;
import com.felipeduan.atendimento.shared.cache.CacheConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

class EmpresaCachePhoneNumberIdIntegrationTest extends AbstractIntegrationTest {

  @Autowired EmpresaService empresaService;
  @Autowired CacheManager cacheManager;

  @Test
  void deveCachearResolucaoDePhoneNumberId() {
    // cria empresa + define phoneNumberId (usar os helpers de EmpresaService/registro que a suíte já usa)
    // ... arrange: criar empresa E com phoneNumberId "pni-cache-1" ...

    empresaService.buscarIdPorPhoneNumberId("pni-cache-1"); // popula
    Object cacheado =
        cacheManager.getCache(CacheConfig.EMPRESA_ID_POR_PHONE_NUMBER_ID).get("pni-cache-1") != null
            ? cacheManager
                .getCache(CacheConfig.EMPRESA_ID_POR_PHONE_NUMBER_ID)
                .get("pni-cache-1")
                .get()
            : null;
    assertThat(cacheado).isNotNull();
  }

  @Test
  void naoDeveCachearPhoneNumberIdDesconhecido() {
    empresaService.buscarIdPorPhoneNumberId("pni-inexistente-" + UUID.randomUUID());
    assertThat(cacheManager.getCache(CacheConfig.EMPRESA_ID_POR_PHONE_NUMBER_ID).get("pni-inexistente"))
        .isNull();
  }

  @Test
  void deveInvalidarAoAtualizarEmpresa() {
    // arrange: empresa com phoneNumberId "pni-cache-2" resolvida (cacheada)
    // act: empresaService.atualizar(id, request que troca o phoneNumberId)
    // assert: cacheManager.getCache(...).get("pni-cache-2") == null  (allEntries evict rodou)
  }
}
```

> **Adapte o arrange** aos helpers reais da suíte (`EmpresaHttpSupport` / criação via service). Não
> invente API — reuse o que os outros testes de empresa já usam para criar empresa com phoneNumberId.

**Gate C2:** `make test-fresh` verde, incluindo os novos testes de cache.

---

### C3 — (opcional, se sobrar tempo) doc

Atualizar `docs/architecture/overview.md` e `docs/decisions/README.md`: hoje dizem "não há uso de
cache". Passa a: *"cache Redis pontual na resolução `phone_number_id → empresaId` (hot path do webhook),
com invalidação em escrita de empresa; nenhum dado de tenant é cacheado — o `tenantKeyGenerator`
fail-closed existe para tornar seguro qualquer cache de tenant futuro."* Um parágrafo em cada.

> **Nota de config de teste (verificar no C1):** os testes precisam de um Redis. Se
> `TestcontainersConfiguration` **não** declara um contêiner Redis com `@ServiceConnection`, adicionar:
> ```java
> @Bean @ServiceConnection(name = "redis")
> RedisContainer redis() { return new RedisContainer(DockerImageName.parse("redis:7")); }
> ```
> (dependência de teste `com.redis:testcontainers-redis` ou o `GenericContainer` com porta 6379 +
> `@ServiceConnection`). Se a app hoje sobe nos testes sem Redis, é porque nada conecta; ao ligar o
> `CacheManager` Redis, o contexto passa a exigir conexão. **Este é o primeiro ponto a validar no C1.**

---

## 3. RATE LIMITING — commits R1–R4

### Estratégia (como os profissionais fazem)

Rate limit em camadas, **cada uma com chave e limite próprios** — não um limite único global:

| Camada | Rota | Chave | Por quê | Limite sugerido (ajustável) |
|---|---|---|---|---|
| **Login** | `/auth/login`, `/auth/plataforma/login` | IP | Anti brute-force de senha | 10 / min |
| **Webhook** | `/webhooks/**` | IP | Endpoint público, alvo de abuso/DoS | 120 / min |
| **Autenticado** | demais rotas de negócio | `tenant_id` (ou userId se sem tenant) | Um tenant não estrangula os outros | 300 / min |

Token-bucket (Bucket4j) — permite rajada até a capacidade e repõe a uma taxa. É o padrão da indústria
para isto.

**Backend distribuído vs local:** como há Redis e a app pode escalar para N instâncias no Dokploy, o
correto é **Redis-backed** (limite global, não por instância — senão um atacante multiplica o limite
batendo em instâncias diferentes). **Mas** há risco de compatibilidade Bucket4j × Lettuce 7 (§5). Por
isso o código fala com a **abstração `ProxyManager<String>` do Bucket4j** — trocar Redis↔local é um
único `@Bean`, sem tocar no interceptor.

### R1 — `build(deps): adiciona Bucket4j`

`build.gradle.kts`:

```kotlin
	implementation("com.bucket4j:bucket4j_jdk17-core:8.14.0")
	implementation("com.bucket4j:bucket4j_jdk17-lettuce:8.14.0")
	implementation("com.bucket4j:bucket4j_jdk17-redis-common:8.14.0")
```

> **Verify-points (obrigatórios no build):**
> 1. **Versão:** `8.14.0` é um pino de referência. Rodar `./gradlew dependencies` e confirmar que
>    resolve; se houver release estável mais nova compatível com Java 25, usar. O group é `com.bucket4j`,
>    os artefatos têm sufixo `_jdk17` (correto para Java 17+, inclui 25).
> 2. **Lettuce 7:** confirmar que `bucket4j_jdk17-lettuce` **não** arrasta um Lettuce 6 conflitante nem
>    quebra contra o Lettuce 7.5 do Boot. Ver §5 para o teste que decide isso e o fallback.

### R2 — `feat(ratelimit): configuração tipada e backend Bucket4j`

#### `shared/ratelimit/RateLimitProperties.java`

```java
package com.felipeduan.atendimento.shared.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
    boolean enabled, Limite login, Limite webhook, Limite autenticado) {

  public record Limite(long capacidade, long porMinuto) {
    public Duration janela() {
      return Duration.ofMinutes(1);
    }
  }
}
```

Registrar em `AtendimentoApplication`:
`@EnableConfigurationProperties({JwtProperties.class, MetaProperties.class, RateLimitProperties.class})`

`application.yaml`:

```yaml
rate-limit:
  enabled: true
  login:        { capacidade: 10,  por-minuto: 10 }
  webhook:      { capacidade: 120, por-minuto: 120 }
  autenticado:  { capacidade: 300, por-minuto: 300 }
```

`application-test.yaml` — desligar por padrão para não poluir a suíte existente; ligar só no teste
dedicado:

```yaml
rate-limit:
  enabled: false
```

#### `shared/ratelimit/ProxyManagerConfig.java` — o backend (Redis primário)

```java
package com.felipeduan.atendimento.shared.ratelimit;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "rate-limit", name = "enabled", havingValue = "true")
public class ProxyManagerConfig {

  // Bucket4j precisa de uma conexão Lettuce com chave String / valor byte[]. O
  // LettuceConnectionFactory do Boot não expõe isso diretamente, então abrimos um RedisClient próprio
  // a partir das mesmas propriedades. Conexão dedicada, fechada no shutdown pelo destroyMethod.
  @Bean(destroyMethod = "shutdown")
  public RedisClient rateLimitRedisClient(
      @Value("${spring.data.redis.host:localhost}") String host,
      @Value("${spring.data.redis.port:6379}") int port,
      @Value("${spring.data.redis.username:}") String username,
      @Value("${spring.data.redis.password:}") String password) {

    RedisURI.Builder uri = RedisURI.builder().withHost(host).withPort(port);
    if (!username.isBlank() && !password.isBlank()) {
      uri.withAuthentication(username, password.toCharArray());
    } else if (!password.isBlank()) {
      uri.withPassword(password.toCharArray());
    }
    return RedisClient.create(uri.build());
  }

  @Bean(destroyMethod = "close")
  public StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(RedisClient client) {
    RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
    return client.connect(codec);
  }

  @Bean
  public ProxyManager<String> proxyManager(StatefulRedisConnection<String, byte[]> connection) {
    return LettuceBasedProxyManager.builderFor(connection)
        .withExpirationStrategy(
            io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
                .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(5)))
        .build();
  }
}
```

> **Este é o arquivo de maior risco.** Os nomes exatos (`LettuceBasedProxyManager.builderFor`,
> `ExpirationAfterWriteStrategy`, o codec) são a API do Bucket4j 8.x. **Cursor:** ao adicionar a dep,
> abrir o javadoc/jar da versão resolvida e confirmar assinatura por assinatura. Se a API divergir,
> ajuste para a da versão. Se **não compilar contra Lettuce 7**, ir para o **fallback in-memory da §5**
> (troca só este arquivo; interceptor e resto ficam idênticos).

#### `shared/ratelimit/RateLimitPolicy.java` — resolve rota → (escopo, limite, chave)

```java
package com.felipeduan.atendimento.shared.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Escolhe qual limite se aplica à requisição e qual chave de bucket usar. */
@Component
public class RateLimitPolicy {

  public record Resolucao(String chave, RateLimitProperties.Limite limite) {}

  private final RateLimitProperties props;

  public RateLimitPolicy(RateLimitProperties props) {
    this.props = props;
  }

  public Optional<Resolucao> resolver(HttpServletRequest request) {
    String path = request.getRequestURI();
    String ip = clientIp(request);

    if (path.equals("/auth/login") || path.equals("/auth/plataforma/login")) {
      return Optional.of(new Resolucao("rl:login:" + ip, props.login()));
    }
    if (path.startsWith("/webhooks/")) {
      return Optional.of(new Resolucao("rl:webhook:" + ip, props.webhook()));
    }
    return chaveAutenticada().map(k -> new Resolucao("rl:auth:" + k, props.autenticado()));
  }

  private Optional<String> chaveAutenticada() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt && jwt.isAuthenticated()) {
      Object tenant = jwt.getToken().getClaim("tenant_id");
      return Optional.of(tenant != null ? tenant.toString() : jwt.getToken().getSubject());
    }
    return Optional.empty();
  }

  // Atrás do proxy do Dokploy/Traefik, o IP real vem no X-Forwarded-For. Configurar também
  // server.forward-headers-strategy=framework (ver R4) para o getRemoteAddr refletir o proxy confiável.
  private String clientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
```

### R3 — `feat(ratelimit): interceptor devolve 429 em ProblemDetail com Retry-After`

#### `shared/ratelimit/RequisicoesExcedidasException.java`

```java
package com.felipeduan.atendimento.shared.ratelimit;

import com.felipeduan.atendimento.shared.exception.DomainException;

public class RequisicoesExcedidasException extends DomainException {

  private final long retryAfterSegundos;

  public RequisicoesExcedidasException(long retryAfterSegundos) {
    super("Muitas requisições. Tente novamente em instantes.");
    this.retryAfterSegundos = retryAfterSegundos;
  }

  public long retryAfterSegundos() {
    return retryAfterSegundos;
  }
}
```

#### `shared/ratelimit/RateLimitInterceptor.java`

```java
package com.felipeduan.atendimento.shared.ratelimit;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

  private final ProxyManager<String> proxyManager;
  private final RateLimitPolicy policy;

  public RateLimitInterceptor(ProxyManager<String> proxyManager, RateLimitPolicy policy) {
    this.proxyManager = proxyManager;
    this.policy = policy;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    var resolucao = policy.resolver(request);
    if (resolucao.isEmpty()) {
      return true; // rota sem política (ex.: actuator) ou não autenticada fora de login/webhook
    }

    RateLimitProperties.Limite limite = resolucao.get().limite();
    var bucket =
        proxyManager.builder().build(resolucao.get().chave(), configuracao(limite));

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      response.addHeader("X-RateLimit-Remaining", Long.toString(probe.getRemainingTokens()));
      return true;
    }

    long retryAfter = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
    throw new RequisicoesExcedidasException(retryAfter);
  }

  private Supplier<BucketConfiguration> configuracao(RateLimitProperties.Limite limite) {
    return () ->
        BucketConfiguration.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(limite.capacidade())
                    .refillGreedy(limite.porMinuto(), Duration.ofMinutes(1))
                    .build())
            .build();
  }
}
```

> **Verify-point:** `Bandwidth.builder().capacity(n).refillGreedy(n, duration).build()` é a API moderna
> do Bucket4j 8.x. Se a versão resolvida usar a forma clássica, é
> `Bandwidth.classic(capacidade, Refill.greedy(porMinuto, Duration.ofMinutes(1)))`. Confirmar contra o
> jar resolvido — **este é o segundo ponto version-sensitive**. `tryConsumeAndReturnRemaining` /
> `ConsumptionProbe` / `getNanosToWaitForRefill` são estáveis.

#### `GlobalExceptionHandler` — handler do 429 (retorna `ResponseEntity` p/ setar Retry-After)

```java
  @ExceptionHandler(RequisicoesExcedidasException.class)
  public ResponseEntity<ProblemDetail> handleRequisicoesExcedidas(RequisicoesExcedidasException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    problem.setTitle("Muitas requisições");
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.retryAfterSegundos()))
        .body(problem);
  }
```

> Imports novos no handler: `org.springframework.http.HttpHeaders`,
> `org.springframework.http.ResponseEntity` (o `ResponseEntity` já é importado; `HttpHeaders` pode
> faltar). O fallback de `DomainException` (422) **não** captura este porque o handler específico vence
> por especificidade — mantido.

### R4 — `feat(ratelimit): registra o interceptor e honra X-Forwarded-For`

#### `shared/ratelimit/RateLimitWebConfig.java`

```java
package com.felipeduan.atendimento.shared.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(prefix = "rate-limit", name = "enabled", havingValue = "true")
public class RateLimitWebConfig implements WebMvcConfigurer {

  private final RateLimitInterceptor interceptor;

  public RateLimitWebConfig(RateLimitInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(interceptor)
        .excludePathPatterns("/actuator/**", "/scalar/**", "/v3/api-docs/**");
  }
}
```

`application.yaml` (raiz, junto de `server:` — criar se não existir): honrar o proxy do Dokploy.

```yaml
server:
  forward-headers-strategy: framework
```

> Isso faz o `getRemoteAddr()` refletir o IP do XFF **do proxy confiável** e evita spoofing direto. O
> `RateLimitPolicy` ainda lê XFF como reforço; em produção atrás do Traefik do Dokploy, o primeiro hop
> é o cliente real.

### R5 — Testes `modules/.../RateLimitIntegrationTest.java`

Ligar o rate-limit só neste teste (`@TestPropertySource(properties = "rate-limit.enabled=true")` +
limites baixos para o teste ser rápido). Provar:

```java
// Pseudoestrutura — adaptar aos helpers HTTP da suíte.
@TestPropertySource(properties = {
    "rate-limit.enabled=true",
    "rate-limit.login.capacidade=3",
    "rate-limit.login.por-minuto=3"
})
class RateLimitIntegrationTest extends AbstractIntegrationTest {

  @Test
  void deveRetornar429AposEstourarLimiteDeLogin() throws Exception {
    // 3 tentativas de /auth/login (credenciais quaisquer) → 401/200 conforme o caso, sem 429
    // 4ª tentativa → 429, header Retry-After presente, corpo application/problem+json, title "Muitas requisições"
  }

  @Test
  void ipsDiferentesTemBucketsIndependentes() throws Exception {
    // estoura com X-Forwarded-For: 1.1.1.1 ; um request com 2.2.2.2 ainda passa
  }

  @Test
  void tenantsDiferentesTemBucketsIndependentes() throws Exception {
    // com rate-limit.autenticado baixo: estoura para o tenant A; tenant B ainda passa
  }
}
```

> **Cross-scope isolation é o teste inegociável do rate limit**, análogo ao teste de isolamento de
> tenant: prova que a chave separa os buckets (IP×IP e tenant×tenant), senão o limite de um pune o outro.

---

## 4. Ordem, custo e o que cortar

| # | Commit | Risco | Corta sob pressão? |
|---|---|---|---|
| C1 | infra de cache + tenantKeyGenerator | baixo (verify serializer) | **não** — é a base segura |
| C2 | cache do phone→empresa + evict | baixo | pode adiar (é otimização) |
| C3 | doc de cache | zero | sim |
| R1 | dep Bucket4j | **médio** (Lettuce 7) | — |
| R2 | proxyManager + properties | **médio** (§5) | — |
| R3 | interceptor + 429 | baixo | **não** se fizer rate limit |
| R4 | registro + forward-headers | baixo | não |
| R5 | testes | baixo | parcial |

Se o tempo apertar e o Lettuce 7 brigar: **cache (C1–C2) entrega valor sozinho e é baixo risco**;
rate-limit vai com o **fallback in-memory da §5** (zero risco de versão), que para deploy single-instance
é equivalente.

---

## 5. Os dois riscos version-sensitive — decisão e fallback

### Risco 1 — Bucket4j-Lettuce × Lettuce 7.5

Bucket4j historicamente compila contra Lettuce 6; o Boot 4.1 trouxe Lettuce 7.5. **Como decidir:** após
R1–R2, rodar o `RateLimitIntegrationTest`. Se subir e os testes passarem → Redis-backed OK, segue.

**Se quebrar** (erro de linkage/API do Lettuce, ou `ProxyManager` não conecta) → **fallback in-memory**,
trocando **somente** `ProxyManagerConfig` por:

```java
@Bean
public ProxyManager<String> proxyManager() {
  // In-memory: limite por instância. Para deploy single-instance (Dokploy 1 contêiner) é equivalente
  // ao Redis. Se escalar para N instâncias, migrar para o backend Lettuce quando a compat resolver.
  return io.github.bucket4j.distributed.proxy.synchronization.SynchronizationStrategy.NONE == null
      ? null
      : buildInMemory();
}
```

Na prática o in-memory do Bucket4j é o `io.github.bucket4j.local`/`ConcurrentHashMap`-based ProxyManager;
o artefato `bucket4j_jdk17-core` já o traz (não precisa do `-lettuce`/`-redis-common`). **Cursor:**
confirmar a classe exata do in-memory ProxyManager na versão resolvida (`javap`/javadoc) — o interceptor
não muda porque fala com `ProxyManager<String>`. Se preferir, remover os artefatos `-lettuce`/
`-redis-common` do `build.gradle.kts` no fallback.

> A abstração `ProxyManager<String>` foi escolhida exatamente para que essa troca seja **um arquivo**.

### Risco 2 — API do `Bandwidth` builder

Ver verify-point no R3. Duas formas conhecidas (builder moderno vs `Bandwidth.classic`+`Refill.greedy`).
Confirmar contra o jar resolvido; é rename local, não redesenho.

---

## 6. Gate final

```
make test-fresh            # tudo verde, incluindo cache + rate limit
# subir a app e provar no curl:
#   - webhook repetido rápido → 429 com Retry-After (se rate-limit.enabled=true local)
#   - GET /actuator/health → UP  (cache Redis não deve derrubar health; se derrubar, ver nota abaixo)
```

> **Health × Redis:** com o cache Redis ligado, o `RedisHealthIndicator` passa a refletir a conexão
> real. Isso é **desejável** agora (o Redis passou a ser usado de verdade). Se o Redis cair, health DOWN
> passa a ser verdade, não ruído — é o comportamento correto. Isso também resolve, pelo lado certo, a
> observação anterior de "health DOWN por Redis não usado": agora ele **é** usado.

---

*Plano escrito sem alterar código de produção. Fatos da §0 verificados contra jars e código; os dois
pontos da §5 são verificações de build que o executor confirma ao adicionar as dependências.*
