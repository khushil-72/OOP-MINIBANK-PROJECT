package model;

/** Practical 6: functional interface used with an anonymous class and a lambda. */
@FunctionalInterface
public interface WithdrawRule {
    boolean allow(Account account, long amount);
}
