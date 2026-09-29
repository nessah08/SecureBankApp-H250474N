package src.bank.service;

import src.bank.exception.BankException;
import src.bank.exception.ValidationException;
import src.bank.model.Account;
import src.bank.model.AccountSummary;
import src.bank.model.AccountType;
import src.bank.model.Transaction;
import src.bank.model.TransactionType;
import src.bank.storage.DataStore;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core banking operations. Every method takes the authenticated username and
 * checks that the account belongs to that user (authorisation), so one user
 * can never read or change another user's account.
 */
public class BankService {
    private static final int MAX_ACCOUNTS_PER_USER = 5;
    private static final int HISTORY_LIMIT = 10;

    private final DataStore store;
    private final Map<String, Account> accounts;
    private final List<Transaction> transactions;
    private final SecureRandom random = new SecureRandom();

    public BankService(DataStore store) throws IOException {
        this.store = store;
        this.accounts = new LinkedHashMap<>(store.loadAccounts());
        this.transactions = new ArrayList<>(store.loadTransactions());
    }

    public synchronized AccountSummary openAccount(String username, AccountType type, BigDecimal initialDeposit)
            throws BankException {
        initialDeposit = initialDeposit.setScale(2, RoundingMode.HALF_UP);
        if (listAccounts(username).size() >= MAX_ACCOUNTS_PER_USER) {
            throw new ValidationException("You can hold at most " + MAX_ACCOUNTS_PER_USER + " accounts.");
        }
        String number = generateAccountNumber();
        Account account = type.create(number, username, initialDeposit);
        accounts.put(number, account);
        try {
            store.saveAccounts(accounts.values());
            if (initialDeposit.signum() > 0) {
                record(account, TransactionType.DEPOSIT, initialDeposit);
            }
        } catch (IOException e) {
            accounts.remove(number);
            throw new BankException("Could not save the new account. Please try again.", e);
        }
        return account.toSummary();
    }

    public synchronized List<AccountSummary> listAccounts(String username) {
        List<AccountSummary> list = new ArrayList<>();
        for (Account a : accounts.values()) {
            if (a.getOwnerUsername().equals(username)) list.add(a.toSummary());
        }
        return list;
    }

    public synchronized BigDecimal getBalance(String username, String accountNumber) throws BankException {
        return requireOwned(username, accountNumber).getBalance();
    }

    public synchronized BigDecimal deposit(String username, String accountNumber, BigDecimal amount)
            throws BankException {
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        Account account = requireOwned(username, accountNumber);
        account.deposit(amount);
        try {
            store.saveAccounts(accounts.values());
            record(account, TransactionType.DEPOSIT, amount);
        } catch (IOException e) {
            account.withdraw(amount);   // roll back the in-memory change
            throw new BankException("Could not save the transaction. No money was moved.", e);
        }
        return account.getBalance();
    }

    public synchronized BigDecimal withdraw(String username, String accountNumber, BigDecimal amount)
            throws BankException {
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        Account account = requireOwned(username, accountNumber);
        account.withdraw(amount);
        try {
            store.saveAccounts(accounts.values());
            record(account, TransactionType.WITHDRAWAL, amount);
        } catch (IOException e) {
            account.deposit(amount);    // roll back the in-memory change
            throw new BankException("Could not save the transaction. No money was moved.", e);
        }
        return account.getBalance();
    }

    /** Most recent transactions (oldest first) for one of the user's accounts. */
    public synchronized List<Transaction> history(String username, String accountNumber) throws BankException {
        requireOwned(username, accountNumber);
        List<Transaction> mine = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.getAccountNumber().equals(accountNumber)) mine.add(t);
        }
        int from = Math.max(0, mine.size() - HISTORY_LIMIT);
        return new ArrayList<>(mine.subList(from, mine.size()));
    }

    // ---------- helpers ----------

    private Account requireOwned(String username, String accountNumber) throws BankException {
        Account a = accounts.get(accountNumber);
        if (a == null || !a.getOwnerUsername().equals(username)) {
            // Same message for "missing" and "not yours" so account numbers cannot be probed.
            throw new BankException("Account not found.");
        }
        return a;
    }

    private void record(Account account, TransactionType type, BigDecimal amount) throws IOException {
        Transaction t = Transaction.create(account.getAccountNumber(), type, amount, account.getBalance());
        store.appendTransaction(t);
        transactions.add(t);
    }

    private String generateAccountNumber() {
        String number;
        do {
            StringBuilder sb = new StringBuilder(10);
            for (int i = 0; i < 10; i++) sb.append(random.nextInt(10));
            number = sb.toString();
        } while (accounts.containsKey(number));
        return number;
    }
}

