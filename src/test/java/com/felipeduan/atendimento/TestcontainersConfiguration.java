package com.felipeduan.atendimento;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("resource")
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  public static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

  public static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

  static {
    POSTGRES.start();
    REDIS.start();
  }

  @Bean
  PostgreSQLContainer postgresContainer() {
    return POSTGRES;
  }

  @Bean
  @ServiceConnection(name = "redis")
  GenericContainer<?> redisContainer() {
    return REDIS;
  }
}
