package com.campus.trade.security;

import com.campus.trade.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ShareGateFilter extends OncePerRequestFilter {

    private final ShareGateSessionService shareGateSessionService;
    private final ObjectMapper objectMapper;

    public ShareGateFilter(ShareGateSessionService shareGateSessionService, ObjectMapper objectMapper) {
        this.shareGateSessionService = shareGateSessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = resolvePath(request);
        String method = request.getMethod();

        if (!path.startsWith("/api/shares")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (HttpMethod.OPTIONS.matches(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!shareGateSessionService.hasValidSession(request)) {
            shareGateSessionService.clearSession(request, response);
            writeForbidden(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolvePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri != null && !requestUri.isBlank()) {
            return requestUri;
        }
        String servletPath = request.getServletPath();
        return servletPath == null ? "" : servletPath;
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(403, "Share gate verify required")));
    }
}
