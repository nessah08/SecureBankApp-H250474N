package src.bank.exception;

/** Thrown when login or registration fails. Messages are deliberately generic. */
public class AuthenticationException extends BankException {
    public AuthenticationException(String message) {
        super(message);
    }
}
