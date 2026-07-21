package com.felipeduan.atendimento.modules.platformadmin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class PlatformAdminSeed implements ApplicationRunner {

    private final AdministradorPlataformaRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${PLATFORM_ADMIN_EMAIL:}")
    private String email;

    @Value("${PLATFORM_ADMIN_NOME:Administrador da Plataforma}")
    private String nome;

    @Value("${PLATFORM_ADMIN_PASSWORD:}")
    private String senha;

    public PlatformAdminSeed(
            AdministradorPlataformaRepository repository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank()) {
            return;
        }

        if (repository.findByEmail(email).isPresent()) {
            return;
        }

        String senhaHash = passwordEncoder.encode(senha);
        
        var administrador = new AdministradorPlataforma(nome, email, senhaHash);
        repository.save(administrador);
    }
}