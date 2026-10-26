package service;

import exception.BankException;
import model.Account;

/**
 * Practical 10: transfers between two accounts while holding both locks.
 * ordered=false locks "from" then "to" (can deadlock when two opposite transfers run);
 * ordered=true always locks the smaller accountNumber first (no deadlock).
 */
public class TransferTask implements Runnable {
    private final Account from;
    private final Account to;
    private final long amount;
    private final boolean ordered;

    public TransferTask(Account from, Account to, long amount, boolean ordered) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.ordered = ordered;
    }

    @Override
    public void run() {
        Account first = from;
        Account second = to;
        if (ordered && from.getAccountNumber().compareTo(to.getAccountNumber()) > 0) {
            first = to;
            second = from;
        }
        String thread = Thread.currentThread().getName();
        synchronized (first) {
            System.out.println(thread + " locked " + first.getAccountNumber());
            try {
                Thread.sleep(200); // give the other thread time to lock its first account
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            System.out.println(thread + " waiting for " + second.getAccountNumber());
            synchronized (second) {
                try {
                    from.withdraw(amount);
                    to.deposit(amount);
                    System.out.println(thread + " moved " + amount + " from " + from.getAccountNumber() + " to "
                            + to.getAccountNumber());
                } catch (BankException e) {
                    System.out.println(thread + " failed: " + e.getMessage());
                }
            }
        }
    }
}
