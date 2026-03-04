package com.campus.trade.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expire-seconds}") long expireSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public String generateToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date exp = Date.from(now.plusSeconds(expireSeconds));

        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("role", role)
                .setIssuedAt(issuedAt)
                .setExpiration(exp)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateShareGateToken(long expireSeconds) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date exp = Date.from(now.plusSeconds(Math.max(60, expireSeconds)));

        return Jwts.builder()
                .setSubject("share-gate")
                .setIssuedAt(issuedAt)
                .setExpiration(exp)
                .claim("scope", "SHARE_GATE")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isValidShareGateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "share-gate".equals(claims.getSubject())
                    && "SHARE_GATE".equals(String.valueOf(claims.get("scope")));
        } catch (Exception ignored) {
            return false;
        }
    }
}
