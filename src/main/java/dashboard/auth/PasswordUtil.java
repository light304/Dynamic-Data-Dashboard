package dashboard.auth;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * PasswordUtil
 * Hashes passwords with SHA-256.
 * Used by AuthService to compare stored hashes against user input.
 */
public class PasswordUtil {

    private PasswordUtil() {}   // utility class — no instantiation

    /**
     * Returns the lowercase hex SHA-256 digest of the given plaintext.
     * @param plaintext raw password string
     * @return 64-character hex string
     */
    public static String sha256(String plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hashBytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
