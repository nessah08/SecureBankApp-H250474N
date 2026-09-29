package src.bank;

import src.bank.service.AuthService;
import src.bank.service.BankService;
import src.bank.storage.DataStore;
import src.bank.storage.FileDataStore;
import src.bank.ui.ConsoleUI;

import java.io.IOException;
import java.nio.file.Paths;

/** Entry point. Optional argument: path of the data directory (default: ./data). */
public class Main {
    public static void main(String[] args) {
        String dir = args.length > 0 ? args[0] : "data";
        try {
            DataStore store = new FileDataStore(Paths.get(dir));
            new ConsoleUI(new AuthService(store), new BankService(store)).run();
        } catch (IOException e) {
            System.err.println("Fatal: could not read or write the data files in '" + dir + "'.");
            System.exit(1);
        }
    }
}
