package src.bank.model;

import java.math.BigDecimal;

public enum AccountType {
    CURRENT,
    CHECKING,
    SAVINGS;

    public Account create(String accountNumber, String ownerUsername, BigDecimal initialBalance) {
        switch (this) {
            case CURRENT:
            case CHECKING:
                return new CurrentAccount(accountNumber, ownerUsername, initialBalance);
            case SAVINGS:
                return new SavingsAccount(accountNumber, ownerUsername, initialBalance);
            default:
                throw new IllegalStateException("Unsupported account type: " + this);
        }
    }
}