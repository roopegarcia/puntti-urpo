import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Local-only setup utility. Never stores a password in source, migrations, or console output. */
class ProvisionAccounts {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize(); // Run from backend/.
        Path env = root.resolve(".env");
        Path credentials = root.resolve(".env.accounts");
        String previous = Files.readString(env);
        boolean configured = previous.lines().filter(line -> line.startsWith("OWNER_PASSWORD_HASH=") || line.startsWith("DEMO_PASSWORD_HASH="))
                .map(line -> line.substring(line.indexOf('=') + 1).strip())
                .anyMatch(value -> !value.isEmpty() && !value.equals("''") && !value.equals("\"\""));
        if (credentials.toFile().exists() || configured) {
            throw new IllegalStateException("Accounts already configured. Preserve existing credentials; see docs/AUTHENTICATION.md for rotation.");
        }
        var random = new SecureRandom();
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String owner = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        random.nextBytes(bytes);
        String demo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var encoder = new BCryptPasswordEncoder(12);
        String configuration = "\nOWNER_PASSWORD_HASH='" + encoder.encode(owner) + "'\nDEMO_PASSWORD_HASH='" + encoder.encode(demo)
                + "'\nSESSION_COOKIE_SECURE=false\n";
        Files.createFile(credentials, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.writeString(credentials, "# LOCAL credentials. Save in your password manager, then remove this file. Never share the owner password.\nOWNER_USERNAME=owner\nOWNER_PASSWORD='" + owner
                + "'\nDEMO_USERNAME=demo\nDEMO_PASSWORD='" + demo + "'\n");
        String preserved = previous.lines().filter(line -> !line.startsWith("OWNER_PASSWORD_HASH=")
                && !line.startsWith("DEMO_PASSWORD_HASH=") && !line.startsWith("SESSION_COOKIE_SECURE="))
                .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
        Files.writeString(env, preserved + configuration);
        Files.setPosixFilePermissions(env, PosixFilePermissions.fromString("rw-------"));
        System.out.println("Local account hashes added to ignored .env; login details saved to ignored .env.accounts (0600). No password was printed.");
    }
}
