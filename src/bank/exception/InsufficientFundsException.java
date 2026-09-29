package src.bank.exception;

/** Thrown when a withdrawal would take an account below its minimum allowed balance. */
public class InsufficientFundsException extends BankException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
