package src.bank.storage;

import src.bank.model.Account;
import src.bank.model.AccountType;
import src.bank.model.Transaction;
import src.bank.model.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain-text file persistence (users.txt, accounts.txt, transactions.txt).
 *
 * Safety measures:
 *  - Writes go to a temp file first and are then atomically moved into place,
 *    so a crash mid-write cannot leave a half-written data file.
 *  - Files are restricted to the owner where the OS supports it.
 *  - Malformed lines are skipped with a warning (the line itself is not printed).
 */
public class FileDataStore implements DataStore {
    private static final String USERS_HEADER = "# username,iterations,saltBase64,hashBase64";
    private static final String ACCOUNTS_HEADER = "# accountNumber,owner,type,balance";
    private static final String TX_HEADER = "# id,timestamp,accountNumber,type,amount,balanceAfter";

    private final Path usersFile;
    private final Path accountsFile;
    private final Path transactionsFile;

    public FileDataStore(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        usersFile = dataDir.resolve("users.txt");
        accountsFile = dataDir.resolve("accounts.txt");
        transactionsFile = dataDir.resolve("transactions.txt");
        createIfMissing(usersFile, USERS_HEADER);
        createIfMissing(accountsFile, ACCOUNTS_HEADER);
        createIfMissing(transactionsFile, TX_HEADER);
    }

    @Override
    public Map<String, User> loadUsers() throws IOException {
        Map<String, User> users = new LinkedHashMap<>();
        for (String line : dataLines(usersFile)) {
            try {
                User u = User.fromRecord(line);
                users.put(u.getUsername(), u);
            } catch (RuntimeException e) {
                warn(usersFile);
            }
        }
        return users;
    }

    @Override
    public void saveUsers(Collection<User> users) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(USERS_HEADER);
        for (User u : users) lines.add(u.toRecord());
        writeAtomically(usersFile, lines);
    }

    @Override
    public Map<String, Account> loadAccounts() throws IOException {
        Map<String, Account> accounts = new LinkedHashMap<>();
        for (String line : dataLines(accountsFile)) {
            try {
                String[] p = line.split(",", -1);
                if (p.length != 4) throw new IllegalArgumentException();
                Account a = AccountType.valueOf(p[2]).create(p[0], p[1], new BigDecimal(p[3]));
                accounts.put(a.getAccountNumber(), a);
            } catch (RuntimeException e) {
                warn(accountsFile);
            }
        }
        return accounts;
    }

    @Override
    public void saveAccounts(Collection<Account> accounts) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(ACCOUNTS_HEADER);
        for (Account a : accounts) lines.add(a.toRecord());
        writeAtomically(accountsFile, lines);
    }

    @Override
    public List<Transaction> loadTransactions() throws IOException {
        List<Transaction> list = new ArrayList<>();
        for (String line : dataLines(transactionsFile)) {
            try {
                list.add(Transaction.fromRecord(line));
            } catch (RuntimeException e) {
                warn(transactionsFile);
            }
        }
        return list;
    }

    @Override
    public void appendTransaction(Transaction t) throws IOException {
        Files.write(transactionsFile, List.of(t.toRecord()), StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
    }

    // ---------- helpers ----------

    private static List<String> dataLines(Path file) throws IOException {
        List<String> out = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) out.add(trimmed);
        }
        return out;
    }

    private static void createIfMissing(Path file, String header) throws IOException {
        if (Files.notExists(file)) {
            writeAtomically(file, List.of(header));
        }
    }

    private static void writeAtomically(Path target, List<String> lines) throws IOException {
        Path tmp = Files.createTempFile(target.toAbsolutePath().getParent(), "bank", ".tmp");
        try {
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            restrictToOwner(tmp);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void restrictToOwner(Path p) {
        try {
            Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Non-POSIX file system (e.g. Windows): skip.
        }
    }

    private static void warn(Path file) {
        System.err.println("Warning: skipped an unreadable record in " + file.getFileName());
    }
}
