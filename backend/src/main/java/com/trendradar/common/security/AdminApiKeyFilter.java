package com.trendradar.common.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AdminApiKeyFilter implements Filter {

    private final String adminApiKey;

    public AdminApiKeyFilter(@Value("${admin.api.key:}") String adminApiKey) {
        this.adminApiKey = adminApiKey;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (!path.startsWith("/api/v1/admin")) {
            chain.doFilter(request, response);
            return;
        }

        String providedKey = httpRequest.getHeader("X-Admin-Key");

        if (adminApiKey.isBlank() || !adminApiKey.equals(providedKey)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(403);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"success\":false,\"message\":\"Forbidden: Invalid admin API key\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
