package src.bank.model;

import java.math.BigDecimal;

/** Current (cheque) account: allows a small fixed overdraft. */
public class CurrentAccount extends Account {
    public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("500.00");

    public CurrentAccount(String accountNumber, String ownerUsername, BigDecimal balance) {
        super(accountNumber, ownerUsername, balance);
    }

    @Override
    public AccountType getType() {
        return AccountType.CURRENT;
    }

    @Override
    protected BigDecimal getMinimumBalance() {
        return OVERDRAFT_LIMIT.negate();
    }
}
