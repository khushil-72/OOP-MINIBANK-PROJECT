package service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import model.Account;

/** Practical 11: save and load all accounts with object serialization. */
public class StatePersister {
    private StatePersister() {
    }

    public static void save(Account[] accounts, Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(accounts);
        }
    }

    public static Account[] load(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Account[] accounts = (Account[]) in.readObject();
            for (Account account : accounts) {
                Account.ensureCounterAtLeast(account.getAccountNumber());
            }
            return accounts;
        }
    }
}
