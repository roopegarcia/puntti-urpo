package com.punttiurpo.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentAccount {
    public String key() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getName().equals("owner") || authentication.getName().equals("demo"))) {
            throw new AccessDeniedException("Kirjaudu sisään.");
        }
        return authentication.getName();
    }
}
