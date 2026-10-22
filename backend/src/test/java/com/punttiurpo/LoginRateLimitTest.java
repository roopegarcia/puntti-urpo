package com.punttiurpo;

import com.punttiurpo.security.LoginRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimitTest {
    @Test
    void capsLoginRequestsAndDoesNotBlockOrdinaryApiReads() throws Exception {
        var filter = new LoginRateLimitFilter();
        for (int attempt = 1; attempt <= 21; attempt++) {
            var request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setServletPath("/api/auth/login");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(204));
            assertThat(response.getStatus()).isEqualTo(attempt <= 20 ? 204 : 429);
            if (attempt == 21) assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        }
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/workouts"), response, (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(204));
        assertThat(response.getStatus()).isEqualTo(204);
    }
}
