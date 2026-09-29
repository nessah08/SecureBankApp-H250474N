package src.bank.security;

import src.bank.exception.ValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Central place for whitelist-style input validation. Everything typed by the
 * user is checked here before it is used, stored or written to a file.
 */
public final class InputValidator {
    public static final BigDecimal MAX_TRANSACTION = new BigDecimal("1000000.00");

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("^[0-9]{10}$");
    private static final Pattern AMOUNT = Pattern.compile("^[0-9]{1,9}(\\.[0-9]{1,2})?$");

    private InputValidator() { }

    public static boolean isValidUsername(String s) {
        return s != null && USERNAME.matcher(s).matches();
    }

    public static String requireUsername(String s) throws ValidationException {
        if (!isValidUsername(s)) {
            throw new ValidationException("Username must be 3-20 characters: letters, digits or underscore.");
        }
        return s;
    }

    public static String requireAccountNumber(String s) throws ValidationException {
        if (s == null || !ACCOUNT_NUMBER.matcher(s.trim()).matches()) {
            throw new ValidationException("Account number must be exactly 10 digits.");
        }
        return s.trim();
    }

    /** Password policy: 8-64 chars with upper, lower, digit and special character. */
    public static void requireStrongPassword(char[] pw) throws ValidationException {
        boolean upper = false, lower = false, digit = false, special = false;
        if (pw == null || pw.length < 8 || pw.length > 64) {
            throw new ValidationException("Password must be 8-64 characters long.");
        }
        for (char c : pw) {
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else if (!Character.isWhitespace(c)) special = true;
        }
        if (!(upper && lower && digit && special)) {
            throw new ValidationException(
                    "Password needs an uppercase letter, a lowercase letter, a digit and a special character.");
        }
    }

    /** Parses a money amount. Rejects negatives, exponents, >2 decimals and absurd sizes. */
    public static BigDecimal parseAmount(String s, boolean allowZero) throws ValidationException {
        if (s == null || !AMOUNT.matcher(s.trim()).matches()) {
            throw new ValidationException("Enter a valid amount, e.g. 150 or 150.75 (max 2 decimals).");
        }
        BigDecimal amount = new BigDecimal(s.trim());
        if (amount.signum() == 0 && !allowZero) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        if (amount.compareTo(MAX_TRANSACTION) > 0) {
            throw new ValidationException("Amount exceeds the per-transaction limit of " + MAX_TRANSACTION + ".");
        }
        return amount;
    }
}
