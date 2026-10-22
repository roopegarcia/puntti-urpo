import java.net.URI;
import java.net.HttpURLConnection;

/** Checks application and PostgreSQL health without installing a second HTTP client in the image. */
class HealthCheck {
    public static void main(String[] args) throws Exception {
        var connection = (HttpURLConnection) URI.create("http://127.0.0.1:8080/actuator/health").toURL().openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(5000);
        try { if (connection.getResponseCode() != 200) System.exit(1); }
        finally { connection.disconnect(); }
    }
}
