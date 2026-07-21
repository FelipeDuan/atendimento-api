package com.felipeduan.atendimento.shared.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.felipeduan.atendimento.shared.config.JwtProperties;

@Service
public class JwtService {

    public static final String CLAIM_TENANT_ID = "tenant_id";
    public static final String CLAIM_AUTHORITIES = "authorities";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public String emitirToken(String subject, Collection<String> authorities, UUID tenantId) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(jwtProperties.expirationMinutes(), ChronoUnit.MINUTES);

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim(CLAIM_AUTHORITIES, authorities);

        if (tenantId != null) {
            claims.claim(CLAIM_TENANT_ID, tenantId.toString());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build()))
                .getTokenValue();
    }
}