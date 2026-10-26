package exception;

public class InvalidAmountException extends BankException {
    private static final long serialVersionUID = 1L;

    public InvalidAmountException(long amount) {
        super("Invalid amount: " + amount + " (must be greater than zero)");
    }
}
