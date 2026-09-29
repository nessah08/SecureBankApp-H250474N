package src.bank.exception;

/** Thrown when user input or a business rule (e.g. amount limits) is violated. */
public class ValidationException extends BankException {
    public ValidationException(String message) {
        super(message);
    }
}