package src.bank.ui;

import src.bank.exception.BankException;
import src.bank.model.AccountSummary;
import src.bank.model.AccountType;
import src.bank.model.Transaction;
import src.bank.security.InputValidator;
import src.bank.service.AuthService;
import src.bank.service.BankService;

import java.io.Console;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Text-menu user interface. Contains no business logic; it only calls the services. */
public class ConsoleUI {
    private final AuthService auth;
    private final BankService bank;
    private final Scanner in = new Scanner(System.in);

    public ConsoleUI(AuthService auth, BankService bank) {
        this.auth = auth;
        this.bank = bank;
    }

    public void run() {
        System.out.println("=== Secure Banking Application ===");
        try {
            boolean running = true;
            while (running) {
                System.out.println("\n1) Login\n2) Register\n3) Exit");
                switch (prompt("Choose an option: ")) {
                    case "1": login(); break;
                    case "2": register(); break;
                    case "3": running = false; break;
                    default: System.out.println("Please choose 1, 2 or 3.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println();   // input stream closed (Ctrl+D / end of piped input)
        }
        System.out.println("Goodbye.");
    }

    // ---------- pre-login ----------

    private void register() {
        String username = prompt("Choose a username (3-20 letters/digits/_): ");
        char[] pw = readPassword("Choose a password (8+ chars, upper, lower, digit, symbol): ");
        char[] confirm = readPassword("Confirm password: ");
        try {
            auth.register(username, pw, confirm);
            System.out.println("Registration successful. You can now log in.");
        } catch (BankException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            Arrays.fill(pw, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    private void login() {
        String username = prompt("Username: ");
        char[] pw = readPassword("Password: ");
        try {
            String user = auth.login(username, pw);
            System.out.println("Welcome, " + user + "!");
            accountMenu(user);
        } catch (BankException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            Arrays.fill(pw, '\0');
        }
    }

    // ---------- post-login ----------

    private void accountMenu(String user) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n1) Open new account\n2) View accounts & balances\n3) Deposit"
                    + "\n4) Withdraw\n5) Transaction history\n6) Logout");
            try {
                switch (prompt("Choose an option: ")) {
                    case "1": openAccount(user); break;
                    case "2": viewAccounts(user); break;
                    case "3": deposit(user); break;
                    case "4": withdraw(user); break;
                    case "5": history(user); break;
                    case "6": loggedIn = false; System.out.println("Logged out."); break;
                    default: System.out.println("Please choose 1-6.");
                }
            } catch (BankException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void openAccount(String user) throws BankException {
        String choice = prompt("Account type - 1) Savings  2) Current: ");
        AccountType type;
        if (choice.equals("1")) type = AccountType.SAVINGS;
        else if (choice.equals("2")) type = AccountType.CURRENT;
        else { System.out.println("Invalid account type."); return; }

        BigDecimal initial = InputValidator.parseAmount(prompt("Initial deposit (0 for none): "), true);
        AccountSummary a = bank.openAccount(user, type, initial);
        System.out.println("Account created. Number: " + a.getAccountNumber()
                + " (" + a.getType() + "), balance " + a.getBalance());
    }

    private void viewAccounts(String user) {
        List<AccountSummary> list = bank.listAccounts(user);
        if (list.isEmpty()) {
            System.out.println("You have no accounts yet. Choose option 1 to open one.");
            return;
        }
        System.out.printf("%-12s %-8s %15s%n", "Account", "Type", "Balance");
        for (AccountSummary a : list) {
            System.out.printf("%-12s %-8s %15s%n", a.getAccountNumber(), a.getType(), a.getBalance());
        }
    }

    private void deposit(String user) throws BankException {
        String number = InputValidator.requireAccountNumber(prompt("Account number: "));
        BigDecimal amount = InputValidator.parseAmount(prompt("Amount to deposit: "), false);
        System.out.println("Deposit successful. New balance: " + bank.deposit(user, number, amount));
    }

    private void withdraw(String user) throws BankException {
        String number = InputValidator.requireAccountNumber(prompt("Account number: "));
        BigDecimal amount = InputValidator.parseAmount(prompt("Amount to withdraw: "), false);
        System.out.println("Withdrawal successful. New balance: " + bank.withdraw(user, number, amount));
    }

    private void history(String user) throws BankException {
        String number = InputValidator.requireAccountNumber(prompt("Account number: "));
        List<Transaction> list = bank.history(user, number);
        if (list.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }
        System.out.printf("%-26s %-11s %12s %14s%n", "Time (UTC)", "Type", "Amount", "Balance after");
        for (Transaction t : list) {
            System.out.printf("%-26s %-11s %12s %14s%n",
                    t.getTimestamp(), t.getType(), t.getAmount(), t.getBalanceAfter());
        }
    }

    // ---------- input helpers ----------

    private String prompt(String message) {
        System.out.print(message);
        return in.nextLine().trim();
    }

    /** Uses the system console (no echo) when available; falls back to visible input in IDEs. */
    private char[] readPassword(String message) {
        Console console = System.console();
        if (console != null) {
            char[] pw = console.readPassword(message);
            if (pw == null) throw new NoSuchElementException();
            return pw;
        }
        System.out.print(message + "(input visible in this terminal) ");
        return in.nextLine().toCharArray();
    }
}
