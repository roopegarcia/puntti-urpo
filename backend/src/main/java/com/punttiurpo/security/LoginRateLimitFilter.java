package com.punttiurpo.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded process-wide login budget; ignores spoofable forwarding headers. No permanent lockout. */
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private long windowStart = System.nanoTime();
    private int attempts;

    private synchronized boolean allow() {
        long now = System.nanoTime();
        if (now - windowStart >= java.time.Duration.ofMinutes(1).toNanos()) {
            windowStart = now;
            attempts = 0;
        }
        return ++attempts <= 20;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getMethod().equals("POST") && request.getServletPath().equals("/api/auth/login") && !allow()) {
            response.setHeader("Retry-After", "60");
            SecurityConfig.problem(response, 429, "Liian monta kirjautumisyritystä. Odota minuutti.");
            return;
        }
        chain.doFilter(request, response);
    }
}
