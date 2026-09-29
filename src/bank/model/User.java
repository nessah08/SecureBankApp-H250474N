package src.bank.model;

import java.util.Base64;

/**
 * A registered user. Only a salted PBKDF2 hash of the password is ever stored,
 * never the password itself. Byte arrays are defensively copied.
 */
public final class User {
    private final String username;
    private final byte[] salt;
    private final byte[] passwordHash;
    private final int iterations;

    public User(String username, byte[] salt, byte[] passwordHash, int iterations) {
        this.username = username;
        this.salt = salt.clone();
        this.passwordHash = passwordHash.clone();
        this.iterations = iterations;
    }

    public String getUsername() { return username; }
    public byte[] getSalt() { return salt.clone(); }
    public byte[] getPasswordHash() { return passwordHash.clone(); }
    public int getIterations() { return iterations; }

    /** File record: username,iterations,saltBase64,hashBase64 */
    public String toRecord() {
        Base64.Encoder enc = Base64.getEncoder();
        return String.join(",", username, Integer.toString(iterations),
                enc.encodeToString(salt), enc.encodeToString(passwordHash));
    }

    /** @throws IllegalArgumentException if the line is malformed */
    public static User fromRecord(String line) {
        String[] p = line.split(",", -1);
        if (p.length != 4) {
            throw new IllegalArgumentException("Bad user record");
        }
        Base64.Decoder dec = Base64.getDecoder();
        return new User(p[0], dec.decode(p[2]), dec.decode(p[3]), Integer.parseInt(p[1]));
    }
}
