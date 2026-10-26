package model;

/** Practical 6: things that earn interest. */
public interface InterestBearing {
    double interestRate();

    long getBalance();

    default double yearlyInterest() {
        return getBalance() * interestRate() / 100.0;
    }
}
