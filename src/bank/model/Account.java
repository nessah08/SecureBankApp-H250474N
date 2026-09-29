package src.bank.model;

import src.bank.exception.BankException;
import src.bank.exception.InsufficientFundsException;
import src.bank.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Abstract base class for all bank accounts.
 *
 * Security/OOP notes:
 *  - Balance is a BigDecimal (never double) to avoid rounding errors with money.
 *  - All fields are private; the balance can only change through deposit()/withdraw().
 *  - Subclasses only decide the minimum allowed balance (polymorphism).
 */
public abstract class Account {
    public static final BigDecimal MAX_BALANCE = new BigDecimal("999999999.99");

    private final String accountNumber;
    private final String ownerUsername;
    private BigDecimal balance;

    protected Account(String accountNumber, String ownerUsername, BigDecimal initialBalance) {
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.ownerUsername = Objects.requireNonNull(ownerUsername);
        this.balance = Objects.requireNonNull(initialBalance).setScale(2, RoundingMode.HALF_UP);
    }

    public abstract AccountType getType();

    /** The lowest balance this account type may reach (0 for savings, negative for overdraft). */
    protected abstract BigDecimal getMinimumBalance();

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public synchronized BigDecimal getBalance() {
        return balance;
    }

    public synchronized void deposit(BigDecimal amount) throws BankException {
        requirePositive(amount);
        BigDecimal newBalance = balance.add(amount);
        if (newBalance.compareTo(MAX_BALANCE) > 0) {
            throw new ValidationException("Deposit would exceed the maximum allowed balance.");
        }
        balance = newBalance.setScale(2, RoundingMode.HALF_UP);
    }

    public synchronized void withdraw(BigDecimal amount) throws BankException {
        requirePositive(amount);
        BigDecimal newBalance = balance.subtract(amount);
        if (newBalance.compareTo(getMinimumBalance()) < 0) {
            throw new InsufficientFundsException("Insufficient funds for this withdrawal.");
        }
        balance = newBalance.setScale(2, RoundingMode.HALF_UP);
    }

    /** An immutable snapshot that is safe to hand to the UI layer. */
    public synchronized AccountSummary toSummary() {
        return new AccountSummary(accountNumber, getType(), balance);
    }

    /** File record: accountNumber,owner,type,balance */
    public synchronized String toRecord() {
        return String.join(",", accountNumber, ownerUsername, getType().name(), balance.toPlainString());
    }

    private static void requirePositive(BigDecimal amount) throws ValidationException {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
    }
}
