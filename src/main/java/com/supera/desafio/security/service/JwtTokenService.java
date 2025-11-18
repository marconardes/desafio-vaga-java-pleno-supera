package com.supera.desafio.security.service;

import com.supera.desafio.core.domain.user.DepartmentType;
import com.supera.desafio.security.config.JwtProperties;
import com.supera.desafio.security.model.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtProperties properties;
    private final Key signingKey;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = buildKey(properties.secret());
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(properties.expirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .setSubject(String.valueOf(user.id()))
                .claim("email", user.email())
                .claim("department", user.department().name())
                .claim("name", user.fullName())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        Long userId = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);
        String name = claims.get("name", String.class);
        DepartmentType department = DepartmentType.valueOf(claims.get("department", String.class));
        return new AuthenticatedUser(userId, email, name, department);
    }

    public long expirationSeconds() {
        return properties.expirationMinutes() * 60;
    }

    private Key buildKey(String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(secretBytes, 0, padded, 0, secretBytes.length);
            for (int i = secretBytes.length; i < 32; i++) {
                padded[i] = (byte) (i * 31);
            }
            secretBytes = padded;
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }
}
