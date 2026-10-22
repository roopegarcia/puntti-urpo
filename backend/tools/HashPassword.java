import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Interactive hash generation for deliberate credential rotation; password is never echoed. */
class HashPassword {
    public static void main(String[] args) {
        var console = System.console();
        if (console == null) throw new IllegalStateException("Run directly in an interactive terminal.");
        char[] first = console.readPassword("New password (14+ characters, at most 72 UTF-8 bytes): ");
        char[] second = console.readPassword("Repeat password: ");
        try {
            String password = new String(first);
            if (!java.util.Arrays.equals(first, second) || password.length() < 14 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("Passwords differ or have an invalid length.");
            }
            console.printf("BCrypt hash (store quoted in private configuration): '%s'%n", new BCryptPasswordEncoder(12).encode(password));
        } finally { java.util.Arrays.fill(first, '\0'); java.util.Arrays.fill(second, '\0'); }
    }
}
