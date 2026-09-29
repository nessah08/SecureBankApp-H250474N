package src.bank.security;

import src.bank.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Password hashing with PBKDF2-HMAC-SHA256, a random per-user salt and a high
 * iteration count. Comparison is constant-time to avoid timing attacks.
 */
public final class PasswordHasher {
    public static final int ITERATIONS = 210_000;   // OWASP guidance for PBKDF2-HMAC-SHA256
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() { }

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] hash(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable on this JVM", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean verify(char[] password, User user) {
        byte[] actual = hash(password, user.getSalt(), user.getIterations());
        return MessageDigest.isEqual(actual, user.getPasswordHash());   // constant-time compare
    }
}

