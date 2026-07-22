package com.felipeduan.atendimento.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meta")
public record MetaProperties(
    String apiUrl, String accessToken, String appSecret, String verifyToken) {}
