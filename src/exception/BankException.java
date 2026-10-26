package exception;

/** Checked base class for every MiniBank error (Practical 8). */
public class BankException extends Exception {
    private static final long serialVersionUID = 1L;

    public BankException(String message) {
        super(message);
    }
}
