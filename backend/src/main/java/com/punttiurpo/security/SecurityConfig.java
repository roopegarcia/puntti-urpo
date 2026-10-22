package com.punttiurpo.security;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    UserDetailsService accounts(@Value("${app.auth.owner-password-hash}") String ownerHash,
                               @Value("${app.auth.demo-password-hash}") String demoHash) {
        // No fallback password, public registration, or password hashes committed to migrations.
        if (!validHash(ownerHash) || !validHash(demoHash) || ownerHash.equals(demoHash)) {
            throw new IllegalArgumentException("Configure two distinct BCrypt password hashes for owner and demo.");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername("owner").password(ownerHash).roles("OWNER").build(),
                User.withUsername("demo").password(demoHash).roles("DEMO").build());
    }

    private static boolean validHash(String hash) {
        return hash.matches("\\$2[aby]\\$(1[0-4])\\$[./A-Za-z0-9]{53}");
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf", "/api/auth/login", "/actuator/health", "/error").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("OWNER")
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                .requestMatchers("/api/auth/logout").authenticated()
                .requestMatchers("/api/**").hasRole("OWNER")
                .anyRequest().denyAll())
            // Session-backed CSRF tokens are retrieved explicitly by React, including after login/logout.
            .formLogin(form -> form.loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, auth) -> response.setStatus(204))
                .failureHandler((request, response, error) -> problem(response, 401, "Käyttäjätunnus tai salasana on virheellinen.")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .deleteCookies("PUNTTI_SESSION")
                .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) -> problem(response, 401, "Kirjaudu sisään."))
                .accessDeniedHandler((request, response, error) -> problem(response, 403, "Toiminto ei ole sallittu. Tarkista kirjautuminen ja päivitä sivu.")))
            .requestCache(cache -> cache.disable())
            .addFilterBefore(new LoginRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    static void problem(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
