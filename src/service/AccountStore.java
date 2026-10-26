package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import exception.AccountNotFoundException;
import model.Account;

/** Practical 12: accounts kept in a HashMap keyed by account number. */
public class AccountStore {
    private final HashMap<String, Account> accounts = new HashMap<>();
    private final ArrayList<String> history = new ArrayList<>();

    public void add(Account account) {
        accounts.put(account.getAccountNumber(), account);
        history.add("ADDED " + account.getAccountNumber());
    }

    public Account get(String accountNumber) throws AccountNotFoundException {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    public ArrayList<Account> getAll() {
        return new ArrayList<>(accounts.values());
    }

    public ArrayList<String> getHistory() {
        return history;
    }

    /** Highest balance first. */
    public ArrayList<Account> sortedByBalance() {
        ArrayList<Account> list = getAll();
        list.sort(Comparator.comparingLong(Account::getBalance).reversed());
        return list;
    }
}
