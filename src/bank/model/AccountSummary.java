package src.bank.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Immutable account details safe to expose to the UI layer. */
public final class AccountSummary {
    private final String accountNumber;
    private final AccountType type;
    private final BigDecimal balance;

    public AccountSummary(String accountNumber, AccountType type, BigDecimal balance) {
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.type = Objects.requireNonNull(type);
        this.balance = Objects.requireNonNull(balance);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}