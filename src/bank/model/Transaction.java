package src.bank.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable record of a single deposit or withdrawal. */
public final class Transaction {
    private final String id;
    private final Instant timestamp;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;

    public Transaction(String id, Instant timestamp, String accountNumber,
                       TransactionType type, BigDecimal amount, BigDecimal balanceAfter) {
        this.id = id;
        this.timestamp = timestamp;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public static Transaction create(String accountNumber, TransactionType type,
                                     BigDecimal amount, BigDecimal balanceAfter) {
        return new Transaction(UUID.randomUUID().toString(), Instant.now(), accountNumber,
                type, amount, balanceAfter);
    }

    public String getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String toRecord() {
        return String.join(",", id, timestamp.toString(), accountNumber, type.name(),
                amount.toPlainString(), balanceAfter.toPlainString());
    }

    public static Transaction fromRecord(String line) {
        String[] fields = line.split(",", -1);
        if (fields.length != 6) {
            throw new IllegalArgumentException("Bad transaction record");
        }
        return new Transaction(fields[0], Instant.parse(fields[1]), fields[2],
                TransactionType.valueOf(fields[3]), new BigDecimal(fields[4]),
                new BigDecimal(fields[5]));
    }
}