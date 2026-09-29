package src.bank.model;

import java.math.BigDecimal;

/** Savings account: the balance may never go below zero. */
public class SavingsAccount extends Account {
    public SavingsAccount(String accountNumber, String ownerUsername, BigDecimal balance) {
        super(accountNumber, ownerUsername, balance);
    }

    @Override
    public AccountType getType() {
        return AccountType.SAVINGS;
    }

    @Override
    protected BigDecimal getMinimumBalance() {
        return BigDecimal.ZERO;
    }
}
