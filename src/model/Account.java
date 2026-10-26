package model;

import java.io.Serializable;

import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.annotation.Id;
import model.annotation.MaxLength;
import model.annotation.Positive;

/**
 * Practicals 2, 3, 5, 6, 7, 8, 9, 11, 12: the base class of every account.
 * The balance can change only through deposit() and withdraw().
 */
public abstract class Account implements Transactable, InterestBearing, Comparable<Account>, Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private static int accountCounter = 0;

    /** Practical 9 demo switch: false lets us watch the race condition, true (normal) is thread-safe. */
    private static volatile boolean threadSafe = true;

    @Id
    private final String accountNumber;
    @MaxLength(30)
    private String ownerName;
    @Positive
    private volatile long balance;
    private boolean active;

    protected Account(String ownerName, long openingBalance) {
        this.accountNumber = generateAccountNumber();
        this.ownerName = ownerName;
        this.balance = openingBalance;
        this.active = true;
    }

    protected Account(String ownerName) {
        this(ownerName, 0);
    }

    private static synchronized String generateAccountNumber() {
        accountCounter++;
        return String.format("AC%04d", accountCounter);
    }

    /** After loading saved accounts, make sure new numbers continue after the loaded ones. */
    public static synchronized void ensureCounterAtLeast(String accountNumber) {
        try {
            int n = Integer.parseInt(accountNumber.substring(2));
            if (n > accountCounter) {
                accountCounter = n;
            }
        } catch (RuntimeException e) {
            // not a number we generated; ignore
        }
    }

    public static void setThreadSafe(boolean value) {
        threadSafe = value;
    }

    // ---- rules each account type must supply (Practical 5) ----
    @Override
    public abstract double interestRate();

    public abstract boolean canWithdraw(long amount);

    /** Lowest balance allowed after a withdrawal; used to work out the shortfall. */
    protected long lowestAllowedBalance() {
        return 0;
    }

    // ---- Transactable (Practicals 2, 8, 9) ----
    @Override
    public void deposit(long amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        if (threadSafe) {
            synchronized (this) {
                balance += amount;
            }
        } else {
            // Unsafe on purpose (Practical 9 demo only): read, pause briefly, then write back,
            // so other threads can slip in between and their updates get lost.
            long current = balance;
            Thread.yield();
            balance = current + amount;
        }
    }

    @Override
    public boolean withdraw(long amount) throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        synchronized (this) {
            if (!canWithdraw(amount)) {
                long shortfall = amount - (balance - lowestAllowedBalance());
                if (shortfall <= 0) {
                    shortfall = amount; // locked or restricted: the whole amount is unavailable
                }
                throw new InsufficientFundsException(shortfall);
            }
            balance -= amount;
            return true;
        }
    }

    /** Practical 8: move money using try, catch and finally; rethrows any BankException. */
    public void transfer(Account to, long amount) throws BankException {
        try {
            withdraw(amount);
            to.deposit(amount);
        } catch (BankException e) {
            System.out.println("Transfer failed: " + e.getMessage());
            throw e;
        } finally {
            System.out.println("Transfer attempt " + accountNumber + " -> " + to.accountNumber + " finished");
        }
    }

    // ---- getters (no setter for balance) ----
    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public long getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    // ---- Object methods (Practical 3) ----
    @Override
    public String toString() {
        return accountNumber + " | " + ownerName + " | " + balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Account)) {
            return false;
        }
        Account other = (Account) o;
        return accountNumber.equals(other.accountNumber);
    }

    @Override
    public int hashCode() {
        return accountNumber.hashCode();
    }

    /** Copy with the same accountNumber (used to test equals in Practical 3). */
    @Override
    public Account clone() {
        try {
            return (Account) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    // ---- Comparable (Practical 12) ----
    @Override
    public int compareTo(Account other) {
        return accountNumber.compareTo(other.accountNumber);
    }
}
