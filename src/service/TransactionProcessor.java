package service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import exception.BankException;
import model.Account;
import model.Command;

/** Practicals 10 and 12: runs transactions on a fixed thread pool over a thread-safe account map. */
public class TransactionProcessor {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

    public void register(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public void submit(Runnable task) {
        executor.execute(task);
    }

    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** Builds a Runnable task that performs one deposit or withdrawal. */
    public Runnable taskFor(Command command) {
        return () -> {
            String thread = Thread.currentThread().getName();
            Account account = accounts.get(command.accountNumber());
            if (account == null) {
                System.out.println(thread + ": account not found " + command.accountNumber());
                return;
            }
            try {
                switch (command.type()) {
                    case DEPOSIT -> account.deposit(command.amount());
                    case WITHDRAW -> account.withdraw(command.amount());
                    case TRANSFER -> System.out.println(thread + ": TRANSFER needs two accounts, skipped");
                }
                System.out.println(thread + " processed " + command.type() + " " + command.accountNumber() + " "
                        + command.amount());
            } catch (BankException e) {
                System.out.println(thread + " failed: " + e.getMessage());
            }
        };
    }
}
