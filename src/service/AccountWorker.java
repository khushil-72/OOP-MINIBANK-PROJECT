package service;

import exception.InvalidAmountException;
import model.Account;

/** Practical 9: deposits into one shared account many times. */
public class AccountWorker implements Runnable {
    private final Account account;
    private final int times;
    private final long amount;

    public AccountWorker(Account account, int times, long amount) {
        this.account = account;
        this.times = times;
        this.amount = amount;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " started");
        try {
            for (int i = 0; i < times; i++) {
                account.deposit(amount);
            }
        } catch (InvalidAmountException e) {
            System.out.println(Thread.currentThread().getName() + ": " + e.getMessage());
        }
        System.out.println(Thread.currentThread().getName() + " finished");
    }
}
