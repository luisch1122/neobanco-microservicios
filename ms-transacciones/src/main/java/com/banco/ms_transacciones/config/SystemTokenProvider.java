package com.banco.ms_transacciones.config;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.banco.common_security.config.TokenJwtConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Component
public class SystemTokenProvider {

    private static final long SYSTEM_USER_ID = 0L;
    private static final String SYSTEM_SUBJECT = "system:ms-transacciones";
    private static final long EXPIRATION_MS = 5 * 60 * 1000L;

    private final SecretKey secretKey;

    public SystemTokenProvider(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public String generarTokenSistema() {

        Claims claims = Jwts.claims()
            .add("userId", SYSTEM_USER_ID)
            .build();

        String token = Jwts.builder()
            .subject(SYSTEM_SUBJECT)
            .claims(claims)
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
            .issuedAt(new Date())
            .signWith(secretKey)
            .compact();

        return TokenJwtConfig.PREFIX_TOKEN + token;
    }

}
