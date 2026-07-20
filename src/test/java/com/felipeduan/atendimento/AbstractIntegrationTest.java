package com.felipeduan.atendimento;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

	@DynamicPropertySource
	static void configurarUsuariosBanco(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", TestcontainersConfiguration.POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", () -> "atendimento_app");
		registry.add("spring.datasource.password", () -> "atendimento");
		registry.add("spring.flyway.user", TestcontainersConfiguration.POSTGRES::getUsername);
		registry.add("spring.flyway.password", TestcontainersConfiguration.POSTGRES::getPassword);
	}

}
