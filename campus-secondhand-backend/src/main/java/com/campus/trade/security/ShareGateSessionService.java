package com.campus.trade.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ShareGateSessionService {

    public static final String COOKIE_NAME = "campus_share_gate";

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<>();

    @Value("${app.share-gate.cookie-secure:false}")
    private boolean cookieSecure;

    public void establishSession(HttpServletRequest request, HttpServletResponse response, long expireSeconds) {
        long ttlSeconds = Math.max(60, expireSeconds);
        long expiresAtMs = System.currentTimeMillis() + ttlSeconds * 1000L;
        cleanupExpired(System.currentTimeMillis());

        String token = generateToken();
        sessions.put(token, new SessionState(expiresAtMs, fingerprintUserAgent(request)));
        writeCookie(response, request, token, ttlSeconds);
    }

    public boolean hasValidSession(HttpServletRequest request) {
        cleanupExpired(System.currentTimeMillis());
        String token = readToken(request);
        if (token == null || token.isBlank()) {
            return false;
        }

        SessionState state = sessions.get(token);
        if (state == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (state.expiresAtMs < now) {
            sessions.remove(token);
            return false;
        }

        return Objects.equals(state.userAgentFingerprint, fingerprintUserAgent(request));
    }

    public void clearSession(HttpServletRequest request, HttpServletResponse response) {
        String token = readToken(request);
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure || request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void writeCookie(HttpServletResponse response, HttpServletRequest request, String token, long ttlSeconds) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure || request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(ttlSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void cleanupExpired(long now) {
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAtMs < now);
    }

    private String fingerprintUserAgent(HttpServletRequest request) {
        String userAgent = String.valueOf(request.getHeader("User-Agent"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userAgent.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return userAgent;
        }
    }

    private record SessionState(long expiresAtMs, String userAgentFingerprint) {
    }
}
