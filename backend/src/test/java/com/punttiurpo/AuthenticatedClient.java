package com.punttiurpo;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import tools.jackson.databind.ObjectMapper;

/** Actual cookie login/CSRF in HTTP integration tests; does not bypass the security chain. */
class AuthenticatedClient {
    static final String PASSWORD = "integration-test-only-password";
    private static final String OWNER_HASH = new BCryptPasswordEncoder(10).encode(PASSWORD);
    private static final String DEMO_HASH = new BCryptPasswordEncoder(10).encode(PASSWORD);
    final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    final String base;
    private final ObjectMapper json = new ObjectMapper();

    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.owner-password-hash", () -> OWNER_HASH);
        registry.add("app.auth.demo-password-hash", () -> DEMO_HASH);
        registry.add("server.servlet.session.cookie.secure", () -> false);
    }

    AuthenticatedClient(int port, String username) throws Exception {
        base = "http://127.0.0.1:" + port;
        var token = csrf();
        var result = http.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-CSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.ofString("username=" + username + "&password=" + PASSWORD)).build(),
                HttpResponse.BodyHandlers.ofString());
        if (result.statusCode() != 204) throw new IllegalStateException("Test login failed: " + result.statusCode());
    }

    String csrf() throws Exception {
        var result = http.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        return json.readTree(result.body()).path("token").asText();
    }

    HttpResponse<String> send(String method, String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
        if (!method.equals("GET")) request.header("X-CSRF-TOKEN", csrf());
        return http.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
