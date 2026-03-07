package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.dto.auth.AuthResponse;
import com.campus.trade.dto.auth.LoginRequest;
import com.campus.trade.dto.auth.MeResponse;
import com.campus.trade.dto.auth.RegisterRequest;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.security.ShareGateSessionService;
import com.campus.trade.service.AuthService;
import com.campus.trade.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final ShareGateSessionService shareGateSessionService;

    @Value("${app.share-gate.passphrase}")
    private String shareGatePassphrase;

    @Value("${app.share-gate.token-expire-seconds:1800}")
    private long shareGateTokenExpireSeconds;

    @Value("${app.share-gate.verify-window-seconds:60}")
    private long shareGateVerifyWindowSeconds;

    @Value("${app.share-gate.verify-max-requests:8}")
    private int shareGateVerifyMaxRequests;

    @Value("${app.share-gate.fail-max-attempts:4}")
    private int shareGateFailMaxAttempts;

    @Value("${app.share-gate.fail-ban-seconds:1800}")
    private long shareGateFailBanSeconds;

    private final ConcurrentHashMap<String, WindowState> verifyWindowMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FailState> failStateMap = new ConcurrentHashMap<>();

    public AuthController(AuthService authService, UserMapper userMapper, ShareGateSessionService shareGateSessionService) {
        this.authService = authService;
        this.userMapper = userMapper;
        this.shareGateSessionService = shareGateSessionService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = extractIp(httpRequest);
        return ApiResponse.ok(authService.login(request, ip));
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        var principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new BadCredentialsException("Unauthorized");
        }
        User user = userMapper.findById(principal.getUserId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BadCredentialsException("Unauthorized");
        }
        return ApiResponse.ok(new MeResponse(user.getId(), user.getUsername(), user.getRole(), user.getAvatarUrl()));
    }

    @GetMapping("/share-gate/status")
    public ApiResponse<Map<String, Boolean>> shareGateStatus(HttpServletRequest request) {
        return ApiResponse.ok(Map.of("verified", shareGateSessionService.hasValidSession(request)));
    }

    @PostMapping("/share-gate/verify")
    public ApiResponse<Map<String, Boolean>> verifyShareGate(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        validateShareGateConfiguration();

        String ip = extractIp(request);
        long now = System.currentTimeMillis();

        verifyRateLimit(ip, now);
        verifyBanState(ip, now);

        String normalizedPassphrase = normalizePassphrase(payload == null ? "" : payload.get("passphrase"));
        if (!StringUtils.hasText(normalizedPassphrase) || !normalizedPassphrase.equals(normalizePassphrase(shareGatePassphrase))) {
            recordFail(ip, now);
            throw new IllegalArgumentException("口令错误");
        }

        clearFail(ip);
        shareGateSessionService.establishSession(request, response, shareGateTokenExpireSeconds);
        return ApiResponse.ok(Map.of("verified", true));
    }

    private void validateShareGateConfiguration() {
        if (!StringUtils.hasText(normalizePassphrase(shareGatePassphrase))) {
            throw new IllegalStateException("分享口令未配置或为空");
        }
    }

    private String normalizePassphrase(String passphrase) {
        return passphrase == null ? "" : passphrase.trim();
    }

    private String extractIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "unknown" : remote;
    }

    private void verifyRateLimit(String ip, long now) {
        long windowMs = Math.max(1, shareGateVerifyWindowSeconds) * 1000L;
        WindowState state = verifyWindowMap.compute(ip, (k, v) -> {
            if (v == null || now - v.windowStartMs >= windowMs) {
                return new WindowState(now, new AtomicInteger(1));
            }
            v.counter.incrementAndGet();
            return v;
        });

        int maxReq = Math.max(1, shareGateVerifyMaxRequests);
        if (state.counter.get() > maxReq) {
            throw new IllegalArgumentException("请求过于频繁，请稍后再试");
        }
    }

    private void verifyBanState(String ip, long now) {
        FailState state = failStateMap.get(ip);
        if (state != null && state.banUntilMs > now) {
            throw new IllegalArgumentException("尝试次数过多，请稍后再试");
        }
    }

    private void recordFail(String ip, long now) {
        int maxAttempts = Math.max(1, shareGateFailMaxAttempts);
        long banMs = Math.max(1, shareGateFailBanSeconds) * 1000L;

        failStateMap.compute(ip, (k, v) -> {
            if (v == null || (v.banUntilMs > 0 && v.banUntilMs <= now)) {
                v = new FailState(0, 0L);
            }
            int next = v.failCount + 1;
            if (next >= maxAttempts) {
                return new FailState(0, now + banMs);
            }
            return new FailState(next, v.banUntilMs);
        });
    }

    private void clearFail(String ip) {
        failStateMap.remove(ip);
    }

    private record WindowState(long windowStartMs, AtomicInteger counter) {
    }

    private record FailState(int failCount, long banUntilMs) {
    }
}
