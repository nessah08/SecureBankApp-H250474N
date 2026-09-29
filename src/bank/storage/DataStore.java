package src.bank.storage;

import src.bank.model.Account;
import src.bank.model.Transaction;
import src.bank.model.User;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DataStore {
    Map<String, User> loadUsers() throws IOException;

    Map<String, Account> loadAccounts() throws IOException;

    List<Transaction> loadTransactions() throws IOException;

    void saveAccounts(Collection<Account> accounts) throws IOException;

    void saveUsers(Collection<User> users) throws IOException;

    void appendTransaction(Transaction transaction) throws IOException;
}