package src.bank.service;

import src.bank.exception.AuthenticationException;
import src.bank.exception.BankException;
import src.bank.exception.ValidationException;
import src.bank.model.User;
import src.bank.security.InputValidator;
import src.bank.security.PasswordHasher;
import src.bank.storage.DataStore;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles registration and login.
 *
 * Security measures:
 *  - Passwords are stored only as salted PBKDF2 hashes.
 *  - Login errors are generic ("Invalid username or password") so an attacker
 *    cannot tell which usernames exist.
 *  - A dummy hash is computed for unknown users so response time does not leak
 *    whether the username exists.
 *  - After MAX_ATTEMPTS failures a username is locked for LOCK_MILLIS.
 */
public class AuthService {
    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCK_MILLIS = 60_000L;
    private static final String GENERIC_FAILURE = "Invalid username or password.";
    private static final byte[] DUMMY_SALT = new byte[16];

    private final DataStore store;
    private final Map<String, User> users;
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, Long> lockedUntil = new HashMap<>();

    public AuthService(DataStore store) throws IOException {
        this.store = store;
        this.users = new HashMap<>(store.loadUsers());
    }

    public synchronized void register(String username, char[] password, char[] confirm) throws BankException {
        InputValidator.requireUsername(username);
        String key = normalise(username);
        InputValidator.requireStrongPassword(password);
        if (!Arrays.equals(password, confirm)) {
            throw new ValidationException("Passwords do not match.");
        }
        if (users.containsKey(key)) {
            throw new ValidationException("That username is not available.");
        }
        byte[] salt = PasswordHasher.newSalt();
        byte[] hash = PasswordHasher.hash(password, salt, PasswordHasher.ITERATIONS);
        User user = new User(key, salt, hash, PasswordHasher.ITERATIONS);
        users.put(key, user);
        try {
            store.saveUsers(users.values());
        } catch (IOException e) {
            users.remove(key);
            throw new BankException("Could not save your registration. Please try again.", e);
        }
    }

    /** @return the authenticated (normalised) username */
    public synchronized String login(String username, char[] password) throws BankException {
        String key = (username == null) ? "" : normalise(username.trim());

        long remaining = lockedUntil.getOrDefault(key, 0L) - System.currentTimeMillis();
        if (remaining > 0) {
            throw new AuthenticationException(
                    "Too many failed attempts. Try again in " + (remaining / 1000 + 1) + " seconds.");
        }

        User user = InputValidator.isValidUsername(key) ? users.get(key) : null;
        boolean ok;
        if (user == null) {
            PasswordHasher.hash(password, DUMMY_SALT, PasswordHasher.ITERATIONS); // equalise timing
            ok = false;
        } else {
            ok = PasswordHasher.verify(password, user);
        }

        if (!ok) {
            int fails = failedAttempts.merge(key, 1, Integer::sum);
            if (fails >= MAX_ATTEMPTS) {
                lockedUntil.put(key, System.currentTimeMillis() + LOCK_MILLIS);
                failedAttempts.remove(key);
            }
            throw new AuthenticationException(GENERIC_FAILURE);
        }
        failedAttempts.remove(key);
        return user.getUsername();
    }

    private static String normalise(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
