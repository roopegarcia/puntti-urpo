package com.punttiurpo.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
    public record CsrfResponse(String token, String headerName) { }
    public record AccountResponse(String username, boolean readOnly) { }

    @GetMapping("/api/auth/csrf")
    public CsrfResponse csrf(CsrfToken token, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @GetMapping("/api/auth/me")
    public AccountResponse me(Authentication authentication) {
        return new AccountResponse(authentication.getName(), authentication.getName().equals("demo"));
    }
}
