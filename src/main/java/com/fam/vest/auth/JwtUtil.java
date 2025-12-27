package com.fam.vest.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class JwtUtil {

    @Value("${fam.vest.app.secret.jwt.enc.key}")
    private String SECRET;

    @Value("${fam.vest.app.jwt.expiry.days:7}")
    private int jwtExpirationInDays;
    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, AtomicBoolean isAdmin) {
        return Jwts.builder()
                .subject(email)
                .issuer("FamVest App")
                .claim("isAdmin", isAdmin.get())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(jwtExpirationInDays, ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = parseToken(token).getPayload();
            return claims.getSubject().equals(username) && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
